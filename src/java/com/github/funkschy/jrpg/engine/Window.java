package com.github.funkschy.jrpg.engine;

import clojure.lang.PersistentHashSet;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class Window {
    private final long windowHandle;
    private final Input input = new Input();

    private int width;
    private int height;

    public Window(int initWidth, int initHeight, String title, boolean vsync) {
        width = initWidth;
        height = initHeight;

        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialiize GLFW");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 2);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);

        windowHandle = glfwCreateWindow(initWidth, initHeight, title, MemoryUtil.NULL, MemoryUtil.NULL);
        if (windowHandle == MemoryUtil.NULL) {
            throw new IllegalStateException("Failed to create GFLW window handle");
        }

        glfwSetFramebufferSizeCallback(windowHandle, (window, w, h) -> {
            width = w;
            height = h;
            glViewport(0, 0, width, height);
        });

        glfwSetKeyCallback(windowHandle, (window, key, scancode, action, mods) -> {
            if (action == GLFW_PRESS) {
                input.keyDown(key);
            } else if (action == GLFW_RELEASE) {
                input.keyUp(key);
            }
        });

        GLFWVidMode vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        if (vidMode != null) {
            // center window
            glfwSetWindowPos(windowHandle, (vidMode.width() - width) / 2, (vidMode.height() - height) / 2);
        }

        glfwMakeContextCurrent(windowHandle);

        if (vsync) {
            glfwSwapInterval(1);
        }

        glfwShowWindow(windowHandle);

        // this line is critical for LWJGL's interop with GLFW's OpenGL context
        // LWJGL detects the context that is current in the current thread, creates the
        // GLCapabilities instance and makes OpenGL bindings available to use
        GL.createCapabilities();
        setClearColor(1f, 0f, 0f, 0f);
    }

    public PersistentHashSet getInputActions() {
        return input.getActions();
    }

    public boolean shouldClose() {
        return glfwWindowShouldClose(windowHandle);
    }

    public boolean isKeyPressed(int keyCode) {
        return glfwGetKey(windowHandle, keyCode) == GLFW_PRESS;
    }

    public void setClearColor(float r, float g, float b, float alpha) {
        glClearColor(r, g, b, alpha);
    }

    public void clear() {
        glClear(GL_COLOR_BUFFER_BIT);
    }

    public void update() {
        glfwSwapBuffers(windowHandle);
        glfwPollEvents();
    }

    public void destroy() {
        glfwDestroyWindow(windowHandle);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

}
