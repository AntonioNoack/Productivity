package me.antonio.noack.maths

import me.antonio.noack.tasks.Scanner

object WordDistance {

    fun calculateDamerauLevenshteinDistance(s1: String, s2: String): Int {
        if(s1 == s2) return 0
        if(s1.isEmpty()) return s2.length
        if(s2.isEmpty()) return s1.length
        val m = s1.length
        val n = s2.length
        val m1 = m+1
        val m2 = m+2
        val data = IntArray((m+1) * (n+1))
        data[0] = 0
        // i,j m,n
        for(i in 0 .. m) data[i] = i
        for(j in 0 .. n) data[j*m1] = j
        for(i in 1 .. m){
            val j = 1
            val ix = i + j*m1
            data[ix] = Scanner.min(
                if (s1[i - 1] == s2[j - 1]) data[ix - m2] else data[ix - m2] + 1,
                data[ix - 1] + 1, data[ix - m1] + 1
            )
        }
        for(j in 2 .. n){
            val i = 1
            val ix = i + j*m1
            data[ix] = Scanner.min(
                if (s1[i - 1] == s2[j - 1]) data[ix - m2] else data[ix - m2] + 1,
                data[ix - 1] + 1, data[ix - m1] + 1
            )
        }
        for(i in 2 .. m){
            for(j in 2 .. n){
                val ix = i + j*m1
                val maybe = Scanner.min(
                    if (s1[i - 1] == s2[j - 1]) data[ix - m2] else data[ix - m2] + 1,
                    data[ix - 1] + 1,
                    data[ix - m1] + 1
                )
                data[ix] = if(s1[i-1] == s2[j-2] && s1[i-2] == s2[j-1]){
                    Scanner.min(maybe, data[(i - 2) + (j - 2) * m1] + 2) // 2 = cost for inverse
                } else maybe
            }
        }
        /* printing the calculated table
        println("$s1/$s2")
        for(i in 0 .. m){
            for(j in 0 .. n){
                val ix = i + j*m1
                print(" %2d".format(data[ix]))
            }
            println()
        }
        * */
        return data.last()
    }

}