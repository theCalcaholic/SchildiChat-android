/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.app.features.home.room.list

import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.amulyakhare.textdrawable.TextDrawable
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.ui.views.ShieldImageView
import im.vector.app.features.home.AvatarRenderer
import org.matrix.android.sdk.api.crypto.RoomEncryptionTrustLevel
import org.matrix.android.sdk.api.util.MatrixItem
import org.w3c.dom.Text

@EpoxyModelClass(layout = R.layout.item_room)
abstract class RoomSummaryItem : VectorEpoxyModel<RoomSummaryItem.Holder>() {

    @EpoxyAttribute lateinit var typingMessage: CharSequence
    @EpoxyAttribute lateinit var avatarRenderer: AvatarRenderer
    @EpoxyAttribute lateinit var matrixItem: MatrixItem

    // Used only for diff calculation
    @EpoxyAttribute lateinit var lastEvent: String

    // We use DoNotHash here as Spans are not implementing equals/hashcode
    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash) lateinit var lastFormattedEvent: CharSequence
    @EpoxyAttribute lateinit var lastEventTime: CharSequence
    @EpoxyAttribute var encryptionTrustLevel: RoomEncryptionTrustLevel? = null
    @EpoxyAttribute var izPublic: Boolean = false
    @EpoxyAttribute var unreadNotificationCount: Int = 0
    @EpoxyAttribute var hasUnreadMessage: Boolean = false
    @EpoxyAttribute var markedUnread: Boolean = false
    @EpoxyAttribute var hasDraft: Boolean = false
    @EpoxyAttribute var showHighlighted: Boolean = false
    @EpoxyAttribute var hasFailedSending: Boolean = false
    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash) var itemLongClickListener: View.OnLongClickListener? = null
    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash) var itemClickListener: View.OnClickListener? = null
    @EpoxyAttribute var showSelected: Boolean = false
    @EpoxyAttribute var showEventPreview: Boolean = false

    override fun bind(holder: Holder) {
        super.bind(holder)

        // TODO: Make configurable via learn path
        val unreadCounterBadgeView: UnreadCounterBadgeView
        val typingView: TextView
        val titleView: TextView
        if (showEventPreview) {
            listOf(
                    holder.nameViewNoEventPreview,
                    holder.unreadCounterBadgeViewNoEventPreview,
                    holder.typingViewNoEventPreview
            ).forEach(holder.rootView::removeView)

            unreadCounterBadgeView = holder.unreadCounterBadgeViewWithEventPreview
            typingView = holder.typingViewWithEventPreview
            titleView = holder.nameViewWithEventPreview

            holder.lastEventView.text = lastFormattedEvent
            holder.lastEventView.isInvisible = typingView.isVisible
        } else {
            listOf(
                    holder.lastEventView,
                    holder.nameViewWithEventPreview,
                    holder.unreadCounterBadgeViewWithEventPreview,
                    holder.typingViewWithEventPreview
            ).forEach(holder.rootView::removeView)

            unreadCounterBadgeView = holder.unreadCounterBadgeViewNoEventPreview
            typingView = holder.typingViewNoEventPreview
            titleView = holder.nameViewNoEventPreview
        }

        holder.rootView.setOnClickListener(itemClickListener)
        holder.rootView.setOnLongClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            itemLongClickListener?.onLongClick(it) ?: false
        }
        titleView.text = matrixItem.getBestName()
        holder.lastEventTimeView.text = lastEventTime
        // SC-TODO: once we count unimportant unread messages, pass that as counter - for now, unreadIndentIndicator is enough, so pass 0 to the badge instead
        unreadCounterBadgeView.render(UnreadCounterBadgeView.State(unreadNotificationCount, showHighlighted, 0, markedUnread))
        holder.unreadIndentIndicator.isVisible = hasUnreadMessage
        holder.draftView.isVisible = hasDraft
        avatarRenderer.render(matrixItem, holder.avatarImageView)
        holder.roomAvatarDecorationImageView.render(encryptionTrustLevel)
        holder.roomAvatarPublicDecorationImageView.isVisible = izPublic
        holder.roomAvatarFailSendingImageView.isVisible = hasFailedSending
        renderSelection(holder, showSelected)
        typingView.setTextOrHide(typingMessage)
    }

    override fun unbind(holder: Holder) {
        holder.rootView.setOnClickListener(null)
        holder.rootView.setOnLongClickListener(null)
        avatarRenderer.clear(holder.avatarImageView)
        super.unbind(holder)
    }

    private fun renderSelection(holder: Holder, isSelected: Boolean) {
        if (isSelected) {
            holder.avatarCheckedImageView.visibility = View.VISIBLE
            val backgroundColor = ColorProvider(holder.view.context).getColor(R.color.riotx_accent)
            val backgroundDrawable = TextDrawable.builder().buildRound("", backgroundColor)
            holder.avatarImageView.setImageDrawable(backgroundDrawable)
        } else {
            holder.avatarCheckedImageView.visibility = View.GONE
            avatarRenderer.render(matrixItem, holder.avatarImageView)
        }
    }

    class Holder() : VectorEpoxyHolder() {
        val unreadCounterBadgeViewWithEventPreview by bind<UnreadCounterBadgeView>(R.id.roomUnreadCounterBadgeView)
        val unreadIndentIndicator by bind<View>(R.id.roomUnreadIndicator)
        val lastEventView by bind<TextView>(R.id.roomLastEventView)
        val typingViewWithEventPreview by bind<TextView>(R.id.roomTypingView)
        val draftView by bind<ImageView>(R.id.roomDraftBadge)
        val lastEventTimeView by bind<TextView>(R.id.roomLastEventTimeView)
        val avatarCheckedImageView by bind<ImageView>(R.id.roomAvatarCheckedImageView)
        val avatarImageView by bind<ImageView>(R.id.roomAvatarImageView)
        val roomAvatarDecorationImageView by bind<ShieldImageView>(R.id.roomAvatarDecorationImageView)
        val roomAvatarPublicDecorationImageView by bind<ImageView>(R.id.roomAvatarPublicDecorationImageView)
        val roomAvatarFailSendingImageView by bind<ImageView>(R.id.roomAvatarFailSendingImageView)
        val rootView by bind<ViewGroup>(R.id.itemRoomLayout)
        val nameViewWithEventPreview by bind<TextView>(R.id.roomNameView)
        //val roomAvatarContainer by bind<FrameLayout>(R.id.roomAvatarContainer)

        val nameViewNoEventPreview by bind<TextView>(R.id.roomNameViewNoEventPreview)
        val unreadCounterBadgeViewNoEventPreview by bind<UnreadCounterBadgeView>(R.id.roomUnreadCounterBadgeViewNoEventPreview)
        val typingViewNoEventPreview by bind<TextView>(R.id.roomTypingViewNoEventView)

//        var nameView = nameViewNoEventPreview
//        var unreadCounterBadgeView = unreadCounterBadgeViewNoEventPreview
//        var typingView = typingViewNoEventPreview

    }
}
