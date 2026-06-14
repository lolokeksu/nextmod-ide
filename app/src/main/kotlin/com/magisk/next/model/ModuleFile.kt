package com.magisk.next.model

data class ModuleFile(
    val name: String,
    var content: String = "",
    var permissions: String = "0644",
    var type: String = "text",
    var isBinary: Boolean = false,
    var binaryContent: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ModuleFile) return false
        return name == other.name && content == other.content &&
                permissions == other.permissions && type == other.type &&
                isBinary == other.isBinary && binaryContent.contentEquals(other.binaryContent)
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + content.hashCode()
        result = 31 * result + permissions.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + isBinary.hashCode()
        result = 31 * result + (binaryContent?.contentHashCode() ?: 0)
        return result
    }
}