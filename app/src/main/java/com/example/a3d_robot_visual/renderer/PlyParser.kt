package com.example.a3d_robot_visual.renderer

import android.content.Context
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

data class PlyModel(
    val vertexBuffer: FloatBuffer,
    val normalBuffer: FloatBuffer,
    val indexBuffer: ShortBuffer,
    val indexCount: Int
)

object PlyParser {

    fun loadFromAssets(context: Context, fileName: String): PlyModel {
        val inputStream = context.assets.open(fileName)
        val header = readHeader(inputStream)

        return when (header.format) {
            PlyFormat.ASCII -> parseAscii(context.assets.open(fileName), header)
            PlyFormat.BINARY_LITTLE_ENDIAN -> parseBinary(context.assets.open(fileName), header)
        }
    }

    // ---------------- HEADER ----------------

    private fun readHeader(input: InputStream): PlyHeader {
        val headerLines = mutableListOf<String>()
        val sb = StringBuilder()
        var prev = -1

        while (true) {
            val curr = input.read()
            if (curr == -1) break
            sb.append(curr.toChar())
            if (curr == '\n'.code && prev == '\r'.code || curr == '\n'.code) {
                val line = sb.toString().trim()
                headerLines.add(line)
                sb.clear()
                if (line == "end_header") break
            }
            prev = curr
        }

        var vertexCount = 0
        var faceCount = 0
        var format = PlyFormat.ASCII

        for (line in headerLines) {
            when {
                line.startsWith("format binary_little_endian") ->
                    format = PlyFormat.BINARY_LITTLE_ENDIAN

                line.startsWith("element vertex") ->
                    vertexCount = line.split(" ")[2].toInt()

                line.startsWith("element face") ->
                    faceCount = line.split(" ")[2].toInt()
            }
        }

        return PlyHeader(format, vertexCount, faceCount)
    }

    // ---------------- ASCII ----------------

    private fun parseAscii(input: InputStream, header: PlyHeader): PlyModel {
        val text = input.bufferedReader().readLines()

        val vertices = mutableListOf<Float>()
        val normals = mutableListOf<Float>()
        val indices = mutableListOf<Short>()

        var index = text.indexOf("end_header") + 1

        repeat(header.vertexCount) {
            val p = text[index++].trim().split("\\s+".toRegex())
            vertices.add(p[0].toFloat())
            vertices.add(p[1].toFloat())
            vertices.add(p[2].toFloat())

            if (p.size >= 6) {
                normals.add(p[3].toFloat())
                normals.add(p[4].toFloat())
                normals.add(p[5].toFloat())
            } else {
                normals.addAll(listOf(0f, 1f, 0f))
            }
        }

        repeat(header.faceCount) {
            val p = text[index++].trim().split("\\s+".toRegex())
            if (p[0] == "3") {
                indices.add(p[1].toShort())
                indices.add(p[2].toShort())
                indices.add(p[3].toShort())
            }
        }

        return buildModel(vertices, normals, indices)
    }

    // ---------------- BINARY ----------------

    private fun parseBinary(input: InputStream, header: PlyHeader): PlyModel {
        val data = input.readBytes()
        val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)

        val vertices = FloatArray(header.vertexCount * 3)
        val normals = FloatArray(header.vertexCount * 3)
        val indices = mutableListOf<Short>()

        for (i in 0 until header.vertexCount) {
            vertices[i * 3] = buffer.float
            vertices[i * 3 + 1] = buffer.float
            vertices[i * 3 + 2] = buffer.float

            normals[i * 3] = buffer.float
            normals[i * 3 + 1] = buffer.float
            normals[i * 3 + 2] = buffer.float
        }

        repeat(header.faceCount) {
            val count = buffer.get().toInt()
            if (count == 3) {
                indices.add(buffer.short)
                indices.add(buffer.short)
                indices.add(buffer.short)
            } else {
                repeat(count) { buffer.int } // skip
            }
        }

        return buildModel(vertices.toList(), normals.toList(), indices)
    }

    // ---------------- BUILD ----------------

    private fun buildModel(
        vertices: List<Float>,
        normals: List<Float>,
        indices: List<Short>
    ): PlyModel {

        val vb = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                vertices.forEach { put(it) }
                position(0)
            }

        val nb = ByteBuffer.allocateDirect(normals.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                normals.forEach { put(it) }
                position(0)
            }

        val ib = ByteBuffer.allocateDirect(indices.size * 2)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
            .apply {
                indices.forEach { put(it) }
                position(0)
            }

        return PlyModel(vb, nb, ib, indices.size)
    }
}

private data class PlyHeader(
    val format: PlyFormat,
    val vertexCount: Int,
    val faceCount: Int
)

private enum class PlyFormat {
    ASCII,
    BINARY_LITTLE_ENDIAN
}