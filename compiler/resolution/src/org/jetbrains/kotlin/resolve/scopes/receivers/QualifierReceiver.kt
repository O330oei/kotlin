/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.resolve.scopes.receivers

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.checker.prepareArgumentTypeRegardingCaptureTypes
import org.jetbrains.kotlin.utils.newLinkedHashSetWithExpectedSize

// this receiver used only for resolution. see subtypes
interface DetailedReceiver

class ReceiverValueWithSmartCastInfo(
    val receiverValue: ReceiverValue,
    val possibleTypes: Set<KotlinType>, // doesn't include receiver.type
    val isStable: Boolean
) : DetailedReceiver {
    override fun toString() = receiverValue.toString()
}

interface QualifierReceiver : Receiver, DetailedReceiver {
    val descriptor: DeclarationDescriptor

    val staticScope: MemberScope

    val classValueReceiver: ReceiverValue?

    // for qualifiers smart cast is impossible
    val classValueReceiverWithSmartCastInfo: ReceiverValueWithSmartCastInfo?
        get() = classValueReceiver?.let { ReceiverValueWithSmartCastInfo(it, emptySet(), true) }
}

fun ReceiverValueWithSmartCastInfo.prepareReceiverRegardingCaptureTypes(): ReceiverValueWithSmartCastInfo {
    val preparedBaseType = prepareArgumentTypeRegardingCaptureTypes(receiverValue.type.unwrap())
    if (preparedBaseType == null && possibleTypes.isEmpty()) return this

    val newPossibleTypes = possibleTypes.mapTo(newLinkedHashSetWithExpectedSize(possibleTypes.size)) {
        prepareArgumentTypeRegardingCaptureTypes(it.unwrap()) ?: it
    }
    val newReceiver = if (preparedBaseType != null) receiverValue.replaceType(preparedBaseType) else receiverValue

    return ReceiverValueWithSmartCastInfo(newReceiver, newPossibleTypes, isStable)
}
