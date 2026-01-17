package com.example.a3d_robot_visual.renderer

import android.content.Context
import java.io.BufferedInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import java.nio.Buffer

data class PlyModel(
    val vertexBuffer: FloatBuffer,
    val normalBuffer: FloatBuffer,
    val indexBuffer: ShortBuffer,
    val indexCount: Int
)

object PlyParser {

    fun loadFromAssets(context: Context, fileName: String): PlyModel {
        val inputStream = BufferedInputStream(context.assets.open(fileName))

        // 1. Read Header
        val header = readHeader(inputStream)

        // 2. Read Data
        return if (header.isBinary) {
            parseBinary(inputStream, header)
        } else {
            // Note: ASCII usually requires the full string,
            // but binary is better for robot models.
            parseAscii(inputStream, header)
        }
    }

    private class InternalHeader(
        val isBinary: Boolean,
        val vertexCount: Int,
        val faceCount: Int,
        val hasNormals: Boolean,
        val propertiesPerVertex: Int
    )

    private fun readHeader(inputStream: BufferedInputStream): InternalHeader {
        var vertexCount = 0
        var faceCount = 0
        var isBinary = false
        var hasNormals = false
        var propertiesPerVertex = 0

        val lineBuffer = StringBuilder()

        while (true) {
            val char = inputStream.read().toChar()
            if (char == '\n') {
                val line = lineBuffer.toString().trim()
                lineBuffer.setLength(0)

                if (line == "end_header") break
                if (line.startsWith("format binary_little_endian")) isBinary = true
                if (line.startsWith("element vertex")) vertexCount = line.split(" ").last().toInt()
                if (line.startsWith("element face")) faceCount = line.split(" ").last().toInt()
                if (line.startsWith("property")) {
                    if (!line.contains("list")) propertiesPerVertex++
                    if (line.contains("nx") || line.contains("normal_x")) hasNormals = true
                }
            } else {
                lineBuffer.append(char)
            }
        }

        return InternalHeader(isBinary, vertexCount, faceCount, hasNormals, propertiesPerVertex)
    }

    private fun parseBinary(inputStream: BufferedInputStream, header: InternalHeader): PlyModel {
        // Read remaining bytes for binary data
        val data = inputStream.readBytes()
        val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)

        val vCoords = FloatArray(header.vertexCount * 3)
        val nCoords = FloatArray(header.vertexCount * 3)
        val indices = ShortArray(header.faceCount * 3)
        var indexCount = 0

        // Parse Vertices
        for (i in 0 until header.vertexCount) {
            vCoords[i * 3] = buffer.getFloat()
            vCoords[i * 3 + 1] = buffer.getFloat()
            vCoords[i * 3 + 2] = buffer.getFloat()

            if (header.hasNormals) {
                nCoords[i * 3] = buffer.getFloat()
                nCoords[i * 3 + 1] = buffer.getFloat()
                nCoords[i * 3 + 2] = buffer.getFloat()
            }

            // Skip extra properties (colors etc)
            val consumed = if (header.hasNormals) 6 else 3
            val skip = (header.propertiesPerVertex - consumed) * 4
            if (skip > 0) (buffer as Buffer).position(buffer.position() + skip)
        }

        // Parse Faces
        repeat(header.faceCount) {
            val count = buffer.get().toInt()
            if (count == 3) {
                // Try reading as Int, but many files use 4-byte Ints for indices
                indices[indexCount++] = buffer.getInt().toShort()
                indices[indexCount++] = buffer.getInt().toShort()
                indices[indexCount++] = buffer.getInt().toShort()
            }
        }

        return buildBuffers(vCoords, nCoords, indices, indexCount)
    }

    private fun parseAscii(inputStream: BufferedInputStream, header: InternalHeader): PlyModel {
        val reader = inputStream.bufferedReader()
        val vCoords = FloatArray(header.vertexCount * 3)
        val nCoords = FloatArray(header.vertexCount * 3)
        val indices = ShortArray(header.faceCount * 3)
        var indexCount = 0

        for (i in 0 until header.vertexCount) {
            val parts = reader.readLine().trim().split(Regex("\\s+"))
            vCoords[i * 3] = parts[0].toFloat()
            vCoords[i * 3 + 1] = parts[1].toFloat()
            vCoords[i * 3 + 2] = parts[2].toFloat()
            if (header.hasNormals && parts.size >= 6) {
                nCoords[i * 3] = parts[3].toFloat()
                nCoords[i * 3 + 1] = parts[4].toFloat()
                nCoords[i * 3 + 2] = parts[5].toFloat()
            }
        }

        repeat(header.faceCount) {
            val parts = reader.readLine().trim().split(Regex("\\s+"))
            if (parts[0] == "3") {
                indices[indexCount++] = parts[1].toShort()
                indices[indexCount++] = parts[2].toShort()
                indices[indexCount++] = parts[3].toShort()
            }
        }

        return buildBuffers(vCoords, nCoords, indices, indexCount)
    }

    private fun buildBuffers(v: FloatArray, n: FloatArray, i: ShortArray, iCount: Int): PlyModel {
        val vb = ByteBuffer.allocateDirect(v.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        vb.put(v).position(0)
        val nb = ByteBuffer.allocateDirect(n.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        nb.put(n).position(0)
        val ib = ByteBuffer.allocateDirect(iCount * 2).order(ByteOrder.nativeOrder()).asShortBuffer()
        ib.put(i, 0, iCount).position(0)
        return PlyModel(vb, nb, ib, iCount)
    }
}