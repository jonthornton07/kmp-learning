package com.thornton.kmpnotes

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform