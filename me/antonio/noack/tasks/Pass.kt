package me.antonio.noack.tasks

import java.io.File

fun getPass(): String {

    val folder = File("/home/antonio/Documents/Wikipedia Data")
    var hash = 0L
    for(file in folder.listFiles().sortedBy { it.name }){
        val name = file.name
        if(!name.startsWith(".")){
            hash = (hash * 16519) + name.hashCode() * file.length()
        }
    }

    return (hash and 0x7fffffffffffffff).toString(16).toUpperCase()

}