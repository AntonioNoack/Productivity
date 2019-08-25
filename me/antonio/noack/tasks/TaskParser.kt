package me.antonio.noack.tasks

class TaskParser(private val path: String){

    private var lastLine: Entry? = null

    val entries = ArrayList<Entry>()
    private var todoIndex = 0

    // todo ich mag dich
    // - so sehr...
    // - umso mehr...
    // todo und so kann der Text länger werden :D
    /**
     * so auch?
     * neee... nicht direkt...
     * */
    fun parse(txt: String){

        var i = 0
        while(i < txt.length){
            when(txt[i]){
                '"' -> i = skipString(txt, i, '"')
                '\'' -> i = skipString(txt, i, '\'')
                '/' -> {
                    i++
                    if(i+1 < txt.length){
                        when(txt[i]){
                            '/' -> {
                                i++
                                val eol = ix(txt, i, '\n')
                                val str = txt.substring(i, eol)
                                parseComment(str)
                                i = eol+1
                            }
                            '*' -> {
                                // /**/
                                val eol = ix(txt, i, "*/")
                                val str = txt.substring(i+1, eol)
                                parseComment(str)
                                i = eol+2
                            }
                        }
                    }
                }
                '\n' -> {
                    i++
                    lastLine = null
                }
                '<' -> {
                    if(txt[i+1] == '-' && txt[i+2] == '-'){
                        // todo xml comment, read until -->
                        var j = i+1
                        while(j+2 < txt.length && !(txt[j] == '-' && txt[j+1] == '-' && txt[j+2] == '>')){
                            j++
                        }
                        val ctx = txt.substring(i+3, j)
                        parseComment(ctx)
                        i = j
                    } else i++
                }
                else -> i++
            }
        }
    }

    private fun trimStars(txt: String): String {
        var str = txt.trim()
        while(str.startsWith("*")){
            str = str.substring(1).trim()
        }
        return str
    }

    private fun parseComment(txt: String){
        val lines = txt.split('\n').map { trimStars(it) }
        for(line in lines){
            val i0 = line.indexOf(' ')+1
            when {
                line.startsWith("todo ") -> {
                    parseTodo(txt.substring(i0).trim(), "todo")
                }
                line.startsWith("done ") -> {
                    parseTodo(txt.substring(i0).trim(), "done")
                }
                line.startsWith("doing ") || line.startsWith("work ") || line.startsWith("coding ") -> {
                    parseTodo(txt.substring(i0).trim(), "doing")
                }
                line.startsWith("fixing ") -> {
                    parseTodo(txt.substring(i0).trim(), "fixing")
                }
                else -> {
                    if(line.isNotEmpty()){
                        val entry = lastLine
                        if(entry != null){
                            entry.taskText += "\n" + line
                        }
                    } else lastLine = null
                }
            }
        }
    }

    private fun parseTodo(txt: String, flag0: String){
        // extract all flags...
        val flags = hashSetOf(flag0)
        val parts = txt.split('#')
        var lastIncludedPart = 0
        for((i, part) in parts.withIndex()){
            if(i > 0){
                val ix = part.indexOf(' ')
                if(ix > 0){
                    lastIncludedPart = i
                    flags.add(part.substring(0, ix))
                } else if(ix != 0){
                    flags.add(part)
                }
            }
        }
        val taskText = parts.subList(0, lastIncludedPart+1).joinToString("#").trim()
        val entry = Entry(-1, -1, taskText, "", path, todoIndex++, flags)
        entries.add(entry)
        lastLine = entry
    }

    private fun ix(txt: String, index0: Int, char: Char): Int {
        val index = txt.indexOf(char, index0)
        return if(index < 0) txt.length else index
    }

    private fun ix(txt: String, index0: Int, char: String): Int {
        val index = txt.indexOf(char, index0)
        return if(index < 0) txt.length else index
    }

    private fun skipString(txt: String, index0: Int, endChar: Char): Int {
        var i = index0+1
        while(i < txt.length){
            when(txt[i]){
                endChar -> return i+1
                '\\' -> i += 2
                else -> i++
            }
        }
        return txt.length
    }

}