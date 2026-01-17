package com.example.a3d_robot_visual.renderer

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class OpenGLRenderer(
    private val context: Context
) : GLSurfaceView.Renderer {

    // Camera
    private val cameraController = CameraController()

    // Matrices
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    // Model buffers
    private lateinit var vertexBuffer: FloatBuffer
    private lateinit var normalBuffer: FloatBuffer
    private lateinit var indexBuffer: ShortBuffer
    private var indexCount = 0

    private var program = 0

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)

        // ---- LOAD PLY MODEL ----
        val model = PlyParser.loadFromAssets(context, "room_model.ply")

        vertexBuffer = model.vertexBuffer
        normalBuffer = model.normalBuffer
        indexBuffer = model.indexBuffer
        indexCount = model.indexCount

        // ---- SHADERS ----
        val vertexShaderCode = """
            uniform mat4 uMVPMatrix;
            uniform mat4 uViewMatrix;
            attribute vec4 aPosition;
            attribute vec3 aNormal;
            varying vec3 vNormal;

            void main() {
                vNormal = mat3(uViewMatrix) * aNormal;
                gl_Position = uMVPMatrix * aPosition;
            }
        """.trimIndent()

        val fragmentShaderCode = """
            precision mediump float;
            varying vec3 vNormal;

            void main() {
                vec3 lightDir = normalize(vec3(0.4, 1.0, 0.3));
                float diff = max(dot(normalize(vNormal), lightDir), 0.0);
                vec3 baseColor = vec3(0.7, 0.7, 0.7);
                vec3 color = baseColor * diff + vec3(0.15);
                gl_FragColor = vec4(color, 1.0);
            }
        """.trimIndent()

        val vs = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fs = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        program = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vs)
            GLES20.glAttachShader(it, fs)
            GLES20.glLinkProgram(it)
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)

        Matrix.perspectiveM(
            projectionMatrix,
            0,
            60f,
            width.toFloat() / height,
            0.1f,
            500f
        )
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        val view = cameraController.getViewMatrix()
        System.arraycopy(view, 0, viewMatrix, 0, 16)

        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        GLES20.glUseProgram(program)

        val posHandle = GLES20.glGetAttribLocation(program, "aPosition")
        val normalHandle = GLES20.glGetAttribLocation(program, "aNormal")
        val mvpHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        val viewHandle = GLES20.glGetUniformLocation(program, "uViewMatrix")

        GLES20.glEnableVertexAttribArray(posHandle)
        GLES20.glVertexAttribPointer(
            posHandle,
            3,
            GLES20.GL_FLOAT,
            false,
            0,
            vertexBuffer
        )

        GLES20.glEnableVertexAttribArray(normalHandle)
        GLES20.glVertexAttribPointer(
            normalHandle,
            3,
            GLES20.GL_FLOAT,
            false,
            0,
            normalBuffer
        )

        GLES20.glUniformMatrix4fv(mvpHandle, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(viewHandle, 1, false, viewMatrix, 0)

        GLES20.glDrawElements(
            GLES20.GL_TRIANGLES,
            indexCount,
            GLES20.GL_UNSIGNED_SHORT,
            indexBuffer
        )

        GLES20.glDisableVertexAttribArray(posHandle)
        GLES20.glDisableVertexAttribArray(normalHandle)
    }

    fun rotateCamera(dx: Float, dy: Float) {
        cameraController.rotate(dx, dy)
    }

    fun zoomCamera(amount: Float) {
        cameraController.zoom(amount)
    }

    private fun loadShader(type: Int, code: String): Int {
        return GLES20.glCreateShader(type).also {
            GLES20.glShaderSource(it, code)
            GLES20.glCompileShader(it)
        }
    }
}