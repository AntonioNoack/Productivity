package me.antonio.noack.tasks

import me.antonio.noack.all.flagDeleted
import me.antonio.noack.maths.WordDistance.calculateDamerauLevenshteinDistance
import me.antonio.noack.tasks.Scanner.isWatchedFile
import java.io.File
import java.io.IOException
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds.*
import java.nio.file.WatchEvent
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.max

class FileWatcher(private val projectName: String,
                  private val srcFolder: File){

    private val wasCreated = HashMap<String, Long>()
    private val fileHashes = HashMap<String, String>()
    private val fileByHash = HashMap<String, String>()

    private val nanoToMillis = 1000L * 1000L

    var maxTaskUID = 0

    private val entriesByFile = HashMap<String, MutableList<Entry>>()

    private fun getNextId(): Int {
        return ++maxTaskUID
    }

    fun initFileList(){
        val time = System.nanoTime()
        Scanner.traverseAll(srcFolder, "", {
                _, path ->
            wasCreated[path] = time
        }, {
                _, _ ->
        })
    }

    private fun addHash(ctx: String, path: String){
        val hash = ctx.hashCode().toString(16).toUpperCase()
        fileHashes[path] = hash
        fileByHash[hash] = path
    }

    private fun refreshContent(ctx: String, path: String){
        // parse the file, collect all entries and check whether entries were removed, added, or changed
        val parser = TaskParser(path)
        parser.parse(ctx)
        val list = parser.entries
        val oldList = entriesByFile[path]
        // println("${oldList?.size} -> ${list.size}")
        if(oldList == null || oldList.isEmpty()){
            // println("old list was empty for $path, $list")
            for(entry in list){
                entry.taskUID = getNextId()
                WebAPI.addEntry(projectName, entry)
            }
        } else if(list.isEmpty()){
            // delete all entries in old
            for(entry in oldList){
                if(entry.todoIndex != -1){
                    WebAPI.addChange(projectName, entry.taskUID, "", path, -1, listOf(flagDeleted))
                }
            }
        } else {
            // check if something was changed
            val newOnes = ArrayList<Entry>()
            val oldLeft = oldList.toHashSet()
            for(entry in list){
                val (smallestError, bestEntry) = compare(entry, oldList)
                // println("error $path $smallestError ($entry, $bestEntry) | ${entry.taskText.startsWith(bestEntry.taskText)} + ${bestEntry.taskText.startsWith(entry.taskText)}")
                when(smallestError) {
                    in -10000 .. 0 -> {
                        // nothing has changed :)
                        entry.taskUID = bestEntry.taskUID
                        oldLeft.remove(bestEntry)
                    }
                    in 1 .. 29 -> {
                        // forgivable error ;)
                        entry.taskUID = bestEntry.taskUID
                        WebAPI.addEntry(projectName, entry)
                        oldLeft.remove(bestEntry)
                    }
                    else -> {
                        // it's a new one
                        entry.taskUID = getNextId()
                        WebAPI.addEntry(projectName, entry)
                        newOnes.add(entry)
                    }
                }
            }
            if(newOnes.isEmpty()){
                for(old in oldLeft){
                    // it's a deleted one
                    WebAPI.addEntryRemoval(projectName, old.taskUID)
                }
            } else {
                for(old in oldLeft){
                    val (smallestError, _) = compare(old, newOnes)
                    // println("error ($old, $bestEntry) = $smallestError | ${old.taskText.startsWith(bestEntry.taskText)} + ${bestEntry.taskText.startsWith(old.taskText)}")
                    when(smallestError){
                        in -10000 .. 0 -> {
                            // nothing has changed :)
                        }
                        in 1 .. 29 -> {
                            // forgivable error ;)
                            // done
                        }
                        else -> {
                            // it's a deleted one
                            WebAPI.addEntryRemoval(projectName, old.taskUID)
                        }
                    }
                }
            }
        }
        entriesByFile[path] = list
    }

    private fun compare(entry: Entry, oldList: List<Entry>): Pair<Int, Entry> {

        var smallestError = 100000
        var bestEntry = oldList.first()
        for(old in oldList){

            val ta = entry.taskText
            val tb = old.taskText

            var error = 0

            if(ta != tb){// not the same

                val ia = ta.indexOf('\n')
                val ib = tb.indexOf('\n')
                val fa = if(ia < 0) ta else ta.substring(0, ia).trim()
                val fb = if(ib < 0) tb else tb.substring(0, ib).trim()
                val ea = if(ia < 0) "" else ta.substring(ia).trim()
                val eb = if(ib < 0) "" else tb.substring(ib).trim()

                val notA = !fa.startsWith(fa)
                val notB = !fb.startsWith(fb)

                if(notA){
                    error += 12
                }

                if(notB){
                    error += 12
                }

                // error gives up to 25 pts for first line, up to 15 for the rest plus first
                error ++
                error += 90 * calculateDamerauLevenshteinDistance(fa, fb) / (fa.length + fb.length + 1)
                error += 60 * calculateDamerauLevenshteinDistance(ea, eb) / (ea.length + eb.length + 1)
            }

            error += abs(entry.taskFlags.size - old.taskFlags.size) * 5

            // check if the flags are the same...
            for(f1 in entry.taskFlags){
                if(!old.taskFlags.contains(f1)){
                    error++
                }
            }

            for(f2 in old.taskFlags){
                if(!entry.taskFlags.contains(f2)){
                    error++
                }
            }

            error += abs(entry.todoIndex - old.todoIndex)

            // println("$error = $ta -> $tb")

            if(error == 0){
                return 0 to old
            } else if(error < smallestError){
                smallestError = error
                bestEntry = old
            }
        }

        return smallestError to bestEntry

    }

    fun addEntryByWeb(entry: Entry){
        maxTaskUID = max(maxTaskUID, entry.taskUID)
        val path = entry.path
        if(wasCreated[path] == null){
            if(entry.todoIndex != -1){
                onDelete(path)
                println("File was no longer found: $path, now all its todos are invalidated")
            } // else deletion is well known :)
        } else {
            // check for changes?... -> no, just wait and then go over all files...
            val list = entriesByFile[path]
            if(list == null){
                entriesByFile[path] = arrayListOf(entry)
            } else {
                list.removeIf { it.taskUID == entry.taskUID }
                if(entry.todoIndex != -1) list.add(entry)
            }
        }
    }

    private fun onDelete(path: String){
        WebAPI.addFileRemoval(projectName, path)
    }

    private fun onRename(from: String, to: String){
        // do I care? idk...
    }

    fun addListener(projectFolder: File, path: String){

        for(file in projectFolder.listFiles()){
            val name = file.name
            if(!name.startsWith(".") && !file.isDirectory && isWatchedFile(name)){
                val ctx = file.readText()
                val childPath = "$path/$name"
                addHash(ctx, childPath)
                refreshContent(ctx, childPath)
            }
        }

        val watcher = FileSystems.getDefault().newWatchService()
        val dir = Path.of(projectFolder.absolutePath)
        try {
            val key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
            thread {
                while(true){
                    watcher.take()
                    for(event in key.pollEvents()){

                        val kind = event.kind()
                        if(kind == OVERFLOW){
                            continue
                        }

                        val name = (event.context() as Path).toString()
                        val kindname = getKindName(kind)

                        if(!name.startsWith(".")){

                            if(Scanner.isWatchedFile(name)){

                                val time = System.nanoTime()
                                val filename = "$path/$name"

                                // default process in Intellij Idea: Delete + Create + Modify
                                // delete has to be included...
                                // create can't create todos, because create creates always empty files -> ignore it :)
                                when(kind){
                                    ENTRY_CREATE -> { // no direct effects, because create creates empty files
                                        wasCreated[filename] = time
                                    }
                                    ENTRY_MODIFY -> {
                                        // scan the file and resolve issues and changes of the to-do lists
                                        wasCreated[filename] = time
                                        refreshContent(File(projectFolder, name).readText(), filename)
                                    }
                                    ENTRY_DELETE -> {
                                        // check whether it was really deleted...
                                        thread {
                                            Thread.sleep(100)
                                            val creationTime = wasCreated[filename]
                                            if(creationTime == null || abs(time - creationTime) > 500 * nanoToMillis){
                                                // was it renamed? :)
                                                val hash = fileHashes[filename]
                                                if(hash == null){
                                                    // somehow not found :/
                                                    onDelete(filename)
                                                } else {
                                                    val secondary = fileByHash[hash]
                                                    if(secondary != null){
                                                        if(secondary == filename){
                                                            // really was deleted
                                                            onDelete(filename)
                                                        } else {
                                                            // it was renamed :D
                                                            // -> a modify should have happened...
                                                            onRename(filename, secondary)
                                                        }
                                                    } else {
                                                        // else crazy
                                                        onDelete(filename)
                                                    }
                                                }
                                            } else {
                                                // it was not deleted, but probably changed; changes are received in ENTRY_MODIFY, so idc for here
                                            }
                                        }
                                    }
                                }

                                println("$kindname: $path/$name")

                            }

                            val file = File(projectFolder, name)
                            if(file.exists() && file.isDirectory){
                                addListener(file, "$path/$name")
                            }
                        }

                    }
                    // important for receiving more events
                    val stillValid = key.reset()
                    if(!stillValid) break
                }
            }
        } catch (e: IOException){
            e.printStackTrace()
        }
    }

    private fun getKindName(kind: WatchEvent.Kind<out Any>?): String {
        return when(kind){
            ENTRY_CREATE -> "create"
            ENTRY_MODIFY -> "modify"
            ENTRY_DELETE -> "delete"
            else -> "unknown/$kind"
        }
    }
}