/*
 * Copyright (C) 2024 Pushlytic
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pushlytic.sdk.model

/**
 * Defines the operations that can be performed on metadata within the Pushlytic SDK.
 *
 * This enum represents the available operations for manipulating metadata:
 * - [UPDATE][MetadataOperationType.UPDATE]: Updates existing metadata or adds new metadata
 * - [CLEAR][MetadataOperationType.CLEAR]: Clears all existing metadata
 *
 * Example usage:
 * ```kotlin
 * val operation = MetadataOperationType.UPDATE
 * val protoValue = operation.protoValue
 * metadataManager.performOperation(operation, metadata)
 * ```
 *
 * @since 1.0.0
 */
enum class MetadataOperationType {
    /**
     * Updates existing metadata or adds new metadata entries.
     */
    UPDATE,

    /**
     * Clears all existing metadata.
     */
    CLEAR;

    /**
     * Converts the operation type to its corresponding Protocol Buffer enumeration value.
     *
     * @return The Protocol Buffer representation of this operation type
     */
    fun toProtoValue(): Int {
        return when (this) {
            UPDATE -> 1
            CLEAR -> 2
        }
    }
}