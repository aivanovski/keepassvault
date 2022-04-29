package com.ivanovsky.passnotes.domain

import com.ivanovsky.passnotes.domain.entity.PasswordResource
import java.security.SecureRandom

class PasswordBuilder(
    private val length: Int,
    private val resources: List<PasswordResource>
) {

    private val items = Array<Item>(length) { NotGeneratedItem }
    private val random = SecureRandom()

    fun restrictSpacesAtStartAndEnd(): PasswordBuilder {
        if (items.isEmpty()) {
            throw IllegalStateException()
        }

        val hasSpace = resources.contains(PasswordResource.SPACE)
        if (!hasSpace) {
            return this
        }

        val resourcesWithoutSpace = resources.filter { it != PasswordResource.SPACE }
        items[0] = ItemFromResources(resourcesWithoutSpace)
        items[length - 1] = ItemFromResources(resourcesWithoutSpace)

        return this
    }

    fun generateOneCharFromEachResource(): PasswordBuilder {
        val availablePositions = items
            .mapIndexedNotNull { idx, item ->
                if (item == NotGeneratedItem) {
                    idx
                } else {
                    null
                }
            }
            .toMutableList()

        for (resource in resources) {
            if (availablePositions.isEmpty()) {
                break
            }

            val position = availablePositions[random.nextInt(availablePositions.size)]
            val char = pickRandomChar(listOf(resource), random)
            availablePositions.remove(position)
            items[position] = GeneratedItem(char)
        }

        return this
    }

    fun generateOtherChars(): PasswordBuilder {
        for (idx in 0 until length) {
            when (val item = items[idx]) {
                is NotGeneratedItem -> {
                    items[idx] = GeneratedItem(pickRandomChar(resources, random))
                }
                is ItemFromResources -> {
                    val char = pickRandomChar(item.resources, random)
                    items[idx] = GeneratedItem(char)
                }
                else -> continue
            }
        }

        return this
    }

    fun build(): String {
        return items
            .mapNotNull { it as? GeneratedItem }
            .map { it.char }
            .joinToString(separator = "")
    }

    private fun pickRandomChar(
        resources: List<PasswordResource>,
        random: SecureRandom
    ): Char {
        val allSymbolCount = resources.sumOf { it.symbols.length }
        val selectedIdx = random.nextInt(allSymbolCount)

        var idx = 0
        for (resource in resources) {
            if (selectedIdx < idx + resource.symbols.length) {
                return resource.symbols[selectedIdx - idx]
            } else {
                idx += resource.symbols.length
            }
        }

        throw IllegalStateException()
    }

    private sealed class Item
    private object NotGeneratedItem : Item()
    private data class ItemFromResources(val resources: List<PasswordResource>) : Item()
    private data class GeneratedItem(val char: Char) : Item()
}