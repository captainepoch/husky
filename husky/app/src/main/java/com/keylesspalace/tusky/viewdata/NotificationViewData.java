/*
 * Husky -- A Pleroma client for Android
 *
 * Copyright (C) 2022  The Husky Developers
 * Copyright (C) 2017  Andrew Dawson
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.keylesspalace.tusky.viewdata;

import com.keylesspalace.tusky.entity.Account;
import com.keylesspalace.tusky.entity.Notification;

import java.util.Objects;

import io.reactivex.annotations.Nullable;

/**
 * Class to represent data required to display either a notification or a placeholder.
 * It is either a {@link Placeholder} or a {@link Concrete}.
 * It is modelled this way because close relationship between placeholder and concrete notification
 * is fine in this case. Placeholder case is not modelled as a type of notification because
 * invariants would be violated and because it would model domain incorrectly. It is prefereable to
 * {@link com.keylesspalace.tusky.util.Either} because class hierarchy is cheaper, faster and
 * more native.
 */
public abstract class NotificationViewData {
    private NotificationViewData() {
    }

    public abstract long getViewDataId();

    public abstract boolean deepEquals(NotificationViewData other);

    public static final class Concrete extends NotificationViewData {
        private final Notification.Type type;
        private final String id;
        private final Account account;
        @Nullable
        private final StatusViewData.Concrete statusViewData;
        @Nullable
        private final String emoji;
        @Nullable
        private final String emojiUrl;
        @Nullable
        private final Account target; // move notification

        public Concrete(Notification.Type type, String id, Account account,
                        @Nullable StatusViewData.Concrete statusViewData,
                        @Nullable String emoji, @Nullable String emojiUrl,
                        @Nullable Account target) {
            this.type = type;
            this.id = id;
            this.account = account;
            this.statusViewData = statusViewData;
            this.emoji = emoji;
            this.emojiUrl = emojiUrl;
            this.target = target;
        }

        public Notification.Type getType() {
            return type;
        }

        public String getId() {
            return id;
        }

        public Account getAccount() {
            return account;
        }

        @Nullable
        public StatusViewData.Concrete getStatusViewData() {
            return statusViewData;
        }

        @Nullable
        public String getEmoji() {
			return emoji;
        }

        @Nullable
        public String getEmojiUrl() {
            return emojiUrl;
        }

        @Nullable
        public Account getTarget() {
            return target;
        }

        @Override
        public long getViewDataId() {
            return id.hashCode();
        }

        @Override
        public boolean deepEquals(NotificationViewData o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Concrete concrete = (Concrete) o;
            return type == concrete.type &&
                    Objects.equals(id, concrete.id) &&
                    account.getId().equals(concrete.account.getId()) &&
                    (emoji != null && concrete.emoji != null && emoji.equals(concrete.emoji)) &&
                    (emojiUrl != null && concrete.emojiUrl != null && emojiUrl.equals(concrete.emojiUrl)) &&
                    (target != null && concrete.target != null && target.getId().equals(concrete.target.getId())) &&
                    (statusViewData == concrete.statusViewData ||
                            statusViewData != null &&
                                    statusViewData.deepEquals(concrete.statusViewData));
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, id, account, statusViewData);
        }
    }

    public static final class Placeholder extends NotificationViewData {
        private final long id;
        private final boolean isLoading;

        public Placeholder(long id, boolean isLoading) {
            this.id = id;
            this.isLoading = isLoading;
        }

        public boolean isLoading() {
            return isLoading;
        }

        @Override
        public long getViewDataId() {
            return id;
        }

        @Override
        public boolean deepEquals(NotificationViewData other) {
            if (!(other instanceof Placeholder)) return false;
            Placeholder that = (Placeholder) other;
            return isLoading == that.isLoading && id == that.id;
        }
    }
}
