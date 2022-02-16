(require '[leiningen.core.eval :refer [get-os]])

(def JVM-OPTS
  {:common  []
   :macosx  ["-XstartOnFirstThread" "-Djava.awt.headless=true"]
   :linux   []
   :windows []})

(defn jvm-opts []
  (->> (JVM-OPTS (get-os))
    (concat (JVM-OPTS :common))
    (distinct)
    (into [])))

(def LWJGL_NS "org.lwjgl")
(def LWJGL_VERSION "3.3.0")
(def LWJGL_MODULES
  ["lwjgl"
   "lwjgl-assimp"
   "lwjgl-glfw"
   "lwjgl-openal"
   "lwjgl-opengl"
   "lwjgl-stb"])

(def LWJGL_PLATFORMS ["linux" "macos" "windows"])

;; These packages don't have any associated native ones.
(def no-natives?
  #{"lwjgl-egl" "lwjgl-jawt" "lwjgl-odbc"
    "lwjgl-opencl" "lwjgl-vulkan"})

(defn lwjgl-deps-with-natives []
  (apply concat
    (for [m LWJGL_MODULES]
      (let [prefix [(symbol LWJGL_NS m) LWJGL_VERSION]]
        (into [prefix]
          (if (no-natives? m)
            []
            (for [p LWJGL_PLATFORMS]
              (into prefix [:classifier (str "natives-" p)
                            :native-prefix ""]))))))))

(def all-dependencies
  (into
    '[[org.clojure/clojure "1.10.1"]]
    (lwjgl-deps-with-natives)))

(defproject jrpg "0.1.0-SNAPSHOT"
  :description "A simple game written in Clojure"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}
  :global-vars {*warn-on-reflection* true}
  :dependencies ~all-dependencies
  :source-paths ["src/clojure"]
  :java-source-paths ["src/java"]
  :jvm-opts ^:replace ~(jvm-opts)
  :main ^:skip-aot com.github.funkschy.jrpg.core
  :repl-options {:init-ns com.github.funkschy.jrpg.core}
  :profiles {:uberjar {:aot :all :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
