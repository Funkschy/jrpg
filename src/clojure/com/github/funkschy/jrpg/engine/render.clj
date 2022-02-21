(ns com.github.funkschy.jrpg.engine.render
  (:require [com.github.funkschy.jrpg.engine.math.m4 :as m4])
  (:import [com.github.funkschy.jrpg.engine Window]
           [java.nio ByteBuffer]
           [org.lwjgl BufferUtils]
           [org.lwjgl.opengl GL30]
           [org.lwjgl.stb STBImage]
           [org.lwjgl.system MemoryStack MemoryUtil]))

(defrecord TextureInfo [texture width height repeat?])
(defrecord Sprite [texture-info src-x src-y src-w src-h])

(defn image-sprite [texture-info]
  (Sprite. texture-info 0 0 (:width texture-info) (:height texture-info)))

(defprotocol Renderer
  (clear-screen [this])
  (screen-dims [this] "Returns [width height] of the screen")
  (draw-sprite [this sprite dest-x dest-y]
    [this sprite dest-x dest-y dest-w dest-h]
    "Draw (part of) a texture to the screen at the specified location")
  (create-texture [this img repeat?] "Create a texture from an image. Returns a TextureInfo instance"))

(defrecord OpenGLRenderer [^Window window program attribs unifs buffers logical-dims])

(defn- orthographic-m4 [w h]
  (m4/orthographic 0 w h 0 400 -400))

(defn- calc-viewport [renderer]
  (let [
        [screen-w screen-h]   (screen-dims renderer)
        [logical-w logical-h] (:logical-dims renderer)

        want-aspect (/ logical-w logical-h)
        real-aspect (/ screen-w screen-h)

        scale (int (max 1 (if (> want-aspect real-aspect)
                            (/ screen-w logical-w)
                            (/ screen-h logical-h))))

        viewport-w (int (Math/floor (* logical-w scale)))
        viewport-h (int (Math/floor (* logical-h scale)))
        viewport-x (/ (- screen-w viewport-w) 2)
        viewport-y (/ (- screen-h viewport-h) 2)]
    [scale viewport-x viewport-y viewport-w viewport-h]))

(extend-type OpenGLRenderer
  Renderer
  (clear-screen [{:keys [^Window window]}]
                                        ; tell webgl how to convert clip space coords into pixels (screen space)
    (. window clear))

  (screen-dims [{^Window window :window}]
    [(. window getWidth) (. window getHeight)])

  (draw-sprite
    ([this {:keys [src-w src-h] :as sprite} dest-x dest-y]
     (draw-sprite this sprite dest-x dest-y src-w src-h))
    ([this sprite dest-x dest-y dest-w dest-h]
     (let [{:keys [program attribs unifs buffers]} this
           {:keys [texture-info src-x src-y src-w src-h]} sprite
           {:keys [texture width height repeat?]} texture-info

           [scale vp-x vp-y vp-w vp-h] (calc-viewport this)

           [tex-coord-mul-w tex-coord-mul-h] (if repeat?
                                               [(/ dest-w src-w) (/ dest-h src-h)]
                                               [1 1])

                                        ; because texture coords go from 0 to 1 and our coords are a unit quad, we can just
                                        ; scale the coord quad
           texture-matrix (m4/multiply (m4/translation (/ src-x width) (/ src-y height) 0)
                                       (m4/scale (* tex-coord-mul-w (/ src-w width))
                                                 (* tex-coord-mul-h (/ src-h height)) 1))
           matrix (m4/multiply (orthographic-m4 vp-w vp-h)
                                        ; move origin into the center of the screen
                               (m4/translation (/ vp-w 2) (/ vp-h 2) 0)
                               (m4/translation (* scale dest-x) (* scale dest-y) 0)
                               (m4/scale       (* scale dest-w) (* scale dest-h) 1)
                                        ; move origin to the middle of the sprite (important for flipping)
                               (m4/translation -0.5 -0.5 0))]

       (GL30/glViewport vp-x vp-y vp-w vp-h)

       (GL30/glBindTexture GL30/GL_TEXTURE_2D texture)
       (GL30/glUseProgram program)

       (GL30/glBindBuffer GL30/GL_ARRAY_BUFFER (buffers "position"))
       (GL30/glEnableVertexAttribArray (attribs "a_position"))
       (GL30/glVertexAttribPointer ^int (attribs "a_position") 2 GL30/GL_FLOAT false 0 0)

       (GL30/glBindBuffer GL30/GL_ARRAY_BUFFER (buffers "texcoord"))
       (GL30/glEnableVertexAttribArray (attribs "a_texcoord"))
       (GL30/glVertexAttribPointer ^int (attribs "a_texcoord") 2 GL30/GL_FLOAT false 0 0)

       (GL30/glUniformMatrix4fv ^int (unifs "u_matrix") false (m4/to-buf matrix))
       (GL30/glUniformMatrix4fv ^int (unifs "u_texture_matrix") false (m4/to-buf texture-matrix))

                                        ; use texture at slot 0
       (GL30/glUniform1i (unifs "u_texture") 0)
       (GL30/glDrawArrays GL30/GL_TRIANGLES 0 6))))

  (create-texture [_ img repeat?]
    (with-open [stack (MemoryStack/stackPush)]
      (let [w (. stack (mallocInt 1))
            h (. stack (mallocInt 1))
            channels (. stack (mallocInt 1))

            buffer (-> (MemoryUtil/memAlloc (count img)) (.put ^bytes img) (.flip))
            buf (STBImage/stbi_load_from_memory ^ByteBuffer buffer w h channels 4)
            _ (MemoryUtil/memFree buffer)

            width (. w get)
            height (. h get)

            texture (GL30/glGenTextures)]

        (GL30/glBindTexture GL30/GL_TEXTURE_2D texture)
                                        ; each component is 1 byte
        (GL30/glPixelStorei GL30/GL_UNPACK_ALIGNMENT 1)
        (when-not repeat?
          (GL30/glTexParameteri GL30/GL_TEXTURE_2D GL30/GL_TEXTURE_WRAP_S GL30/GL_CLAMP_TO_EDGE)
          (GL30/glTexParameteri GL30/GL_TEXTURE_2D GL30/GL_TEXTURE_WRAP_T GL30/GL_CLAMP_TO_EDGE))
        (GL30/glTexParameteri GL30/GL_TEXTURE_2D GL30/GL_TEXTURE_MIN_FILTER GL30/GL_NEAREST)
        (GL30/glTexParameteri GL30/GL_TEXTURE_2D GL30/GL_TEXTURE_MAG_FILTER GL30/GL_NEAREST)

        (GL30/glTexImage2D GL30/GL_TEXTURE_2D 0 GL30/GL_RGBA width height 0 GL30/GL_RGBA GL30/GL_UNSIGNED_BYTE buf)
        (STBImage/stbi_image_free buf)

        (TextureInfo. texture width height repeat?)))))

(defn- create-shader [kind source]
  (let [shader (GL30/glCreateShader kind)]
    (GL30/glShaderSource shader ^CharSequence source)
    (GL30/glCompileShader shader)

    (if (zero? (GL30/glGetShaderi shader GL30/GL_COMPILE_STATUS))
      (do (println (GL30/glGetShaderInfoLog shader))
          (GL30/glDeleteShader shader)
          (throw (IllegalStateException. "Could not compile shader")))
      shader)))

(defn- create-program [vertex-shader fragment-shader]
  (let [program (GL30/glCreateProgram)]
    (GL30/glAttachShader program vertex-shader)
    (GL30/glAttachShader program fragment-shader)
    (GL30/glLinkProgram program)

    (if (zero? (GL30/glGetProgrami program GL30/GL_LINK_STATUS))
      (do (println (GL30/glGetProgramInfoLog program))
          (GL30/glDeleteProgram program)
          (throw (IllegalStateException. "Could not link program")))
      program)))

(defn- create-vertex-shader [source]
  (create-shader GL30/GL_VERTEX_SHADER source))

(defn- create-fragment-shader [source]
  (create-shader GL30/GL_FRAGMENT_SHADER source))

(defn- find-locations [getter & names]
  (reduce (fn [locations n] (assoc locations n (getter n))) {} names))

(defn- find-attrib-locations [program & attrib-names]
  (apply find-locations #(GL30/glGetAttribLocation ^Long program ^CharSequence %) attrib-names))

(defn- find-unif-locations [program & unif-names]
  (apply find-locations #(GL30/glGetUniformLocation ^Long program ^CharSequence %) unif-names))

(def unit-quad (float-array [0 0, 0 1, 1 0, 1 0, 0 1, 1 1]))

(defn- create-unit-quad-buffers [& names]
  (let [vao (GL30/glGenVertexArrays)]
    (GL30/glBindVertexArray vao)
    (reduce
     (fn [m n]
       (let [buffer-vbo (GL30/glGenBuffers)
             buffer (-> (BufferUtils/createFloatBuffer (count unit-quad))
                        (.put ^floats unit-quad)
                        (.flip))]
         (GL30/glBindBuffer GL30/GL_ARRAY_BUFFER buffer-vbo)
         (GL30/glBufferData GL30/GL_ARRAY_BUFFER buffer GL30/GL_STATIC_DRAW)
         (assoc m n buffer-vbo)))
     {}
     names)))

(defn create-renderer [window vs-src fs-src logical-dims]
  (let [vs (create-vertex-shader vs-src)
        fs (create-fragment-shader fs-src)
        program (create-program vs fs)
                                        ; lookup locations in init code, not in render loop
        attribs (find-attrib-locations program "a_position" "a_texcoord")
        unifs (find-unif-locations program "u_matrix" "u_texture" "u_texture_matrix")
        buffers (create-unit-quad-buffers "position" "texcoord")]
    (GL30/glBlendFunc GL30/GL_SRC_ALPHA GL30/GL_ONE_MINUS_SRC_ALPHA)
    (GL30/glEnable GL30/GL_BLEND)
    (OpenGLRenderer. window program attribs unifs buffers logical-dims)))
