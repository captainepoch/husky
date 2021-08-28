/* Copyright 2020 Alibek Omarov
 * 
 * This file is a part of Husky.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * Tusky is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Tusky; if not,
 * see <http://www.gnu.org/licenses>. */

package com.keylesspalace.tusky.entity

import com.google.gson.annotations.SerializedName
import java.util.*

// .well-known/nodeinfo
data class NodeInfoLink(
    val href: String,
    val rel: String
)

data class NodeInfoLinks(
    val links: List<NodeInfoLink>
)

// we care only about supported postFormats
// so implement only metadata fetching
data class NodeInfo(
    val metadata: NodeInfoMetadata? = null,
    val software: NodeInfoSoftware
)

data class NodeInfoSoftware(
    val name: String,
    val version: String
)

data class NodeInfoPleromaUploadLimits(
    val avatar: Long?,
    val background: Long?,
    val banner: Long?,
    val general: Long?
)

data class NodeInfoPixelfedUploadLimits(
    @SerializedName("max_photo_size") val maxPhotoSize: Long?
)

data class NodeInfoPixelfedConfig(
    val uploader: NodeInfoPixelfedUploadLimits?
)

data class NodeInfoMetadata(
    val postFormats: List<String>?,
    val uploadLimits: NodeInfoPleromaUploadLimits?,
    val config: NodeInfoPixelfedConfig?
)

