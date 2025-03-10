/*
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.query

import io.realm.RealmQuery
import io.realm.Sort
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.RoomSortOrder
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntity
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntityFields
import timber.log.Timber
import java.util.EnumSet

internal fun RealmQuery<RoomSummaryEntity>.process(sortOrder: EnumSet<RoomSortOrder>): RealmQuery<RoomSummaryEntity> {

    val fields = mutableListOf<String>()
    val sortOrders = mutableListOf<Sort>()

    Timber.i("Sorting by: ${sortOrder.joinToString(", ")}")

    if (sortOrder.contains(RoomSortOrder.UNREAD)) {
        Timber.w("SORTING FOR UNREAD MSGS!")
        fields.add(RoomSummaryEntityFields.HAS_UNREAD_MESSAGES)
        sortOrders.add(Sort.DESCENDING)
    }
    if (sortOrder.contains(RoomSortOrder.NAME)) {
        fields.add(RoomSummaryEntityFields.DISPLAY_NAME)
        sortOrders.add(Sort.ASCENDING)
    }
    if (sortOrder.contains(RoomSortOrder.ACTIVITY)) {
        fields.add(RoomSummaryEntityFields.LAST_ACTIVITY_TIME)
        sortOrders.add(Sort.DESCENDING)
    }

    Timber.i("fields size: ${fields.size}")
    Timber.i("fields: ${fields.joinToString(", ")}")
    Timber.i("sortOrders size: ${sortOrders.size}")

//    fields.zip(sortOrders).forEach{pair ->
//        sort(pair.first, pair.second)
//    }
    if (fields.size != 0)
        sort(fields.toTypedArray(), sortOrders.toTypedArray())

    return this
}
