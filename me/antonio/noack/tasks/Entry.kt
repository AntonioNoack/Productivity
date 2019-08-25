package me.antonio.noack.tasks

class Entry(var taskUID: Int,
            val version: Int,
            var taskText: String,
            val created: String,
            val path: String,
            val todoIndex: Int,
            val taskFlags: HashSet<String>){

    override fun toString(): String {
        return "#$taskUID.$version: ${taskText.replace("\n", "\\n")}, ${taskFlags.joinToString(" "){"#$it"}}"
    }

}