/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.home.room.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.RecyclerView
import im.vector.app.R
import im.vector.app.core.utils.DebouncedClickListener
import im.vector.app.databinding.ItemRoomCategoryBinding
import im.vector.app.features.themes.ThemeUtils

class SectionHeaderAdapter constructor(
        private val onClickAction: (() -> Unit)
) : RecyclerView.Adapter<SectionHeaderAdapter.VH>() {

    data class RoomsSectionData(
            val name: String,
            val isExpanded: Boolean = true,
            val notificationCount: Int = 0,
            val isHighlighted: Boolean = false,
            val isHidden: Boolean = true,
            // SC additions
            val unread: Int = 0,
            val markedUnread: Boolean = false,

            // This will be false until real data has been submitted once
            val isLoading: Boolean = true
    )

    lateinit var roomsSectionData: RoomsSectionData
        private set

    fun updateSection(newRoomsSectionData: RoomsSectionData) {
        if (!::roomsSectionData.isInitialized || newRoomsSectionData != roomsSectionData) {
            roomsSectionData = newRoomsSectionData
            notifyDataSetChanged()
        }
    }

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int) = roomsSectionData.hashCode().toLong()

    override fun getItemViewType(position: Int) = R.layout.item_room_category

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH.create(parent, this.onClickAction)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(roomsSectionData)
    }

    override fun getItemCount(): Int = if (roomsSectionData.isHidden) 0 else 1

    class VH constructor(
            private val binding: ItemRoomCategoryBinding,
            onClickAction: (() -> Unit)
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener(DebouncedClickListener({
                onClickAction.invoke()
            }))
        }

        fun bind(roomsSectionData: RoomsSectionData) {
            binding.roomCategoryTitleView.text = roomsSectionData.name
            val tintColor = ThemeUtils.getColor(binding.root.context, R.attr.riotx_text_secondary)
            val expandedArrowDrawableRes = if (roomsSectionData.isExpanded) R.drawable.ic_expand_more_white else R.drawable.ic_expand_less_white
            val expandedArrowDrawable = ContextCompat.getDrawable(binding.root.context, expandedArrowDrawableRes)?.also {
                DrawableCompat.setTint(it, tintColor)
            }
            binding.roomCategoryUnreadCounterBadgeView.render(UnreadCounterBadgeView.State(roomsSectionData.notificationCount, roomsSectionData.isHighlighted, roomsSectionData.unread, roomsSectionData.markedUnread))
            binding.roomCategoryTitleView.setCompoundDrawablesWithIntrinsicBounds(null, null, expandedArrowDrawable, null)
        }

        companion object {
            fun create(parent: ViewGroup, onClickAction: () -> Unit): VH {
                val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_room_category, parent, false)
                val binding = ItemRoomCategoryBinding.bind(view)
                return VH(binding, onClickAction)
            }
        }
    }
}
