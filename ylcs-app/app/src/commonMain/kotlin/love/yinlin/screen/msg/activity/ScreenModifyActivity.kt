package love.yinlin.screen.msg.activity

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import kotlinx.datetime.LocalDate
import kotlinx.io.files.Path
import kotlinx.io.readByteArray
import love.yinlin.api.*
import love.yinlin.app
import love.yinlin.compose.CustomTheme
import love.yinlin.compose.Device
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.data.compose.ImageQuality
import love.yinlin.compose.graphics.PlatformImage
import love.yinlin.compose.graphics.crop
import love.yinlin.compose.graphics.decode
import love.yinlin.compose.graphics.encode
import love.yinlin.compose.graphics.thumbnail
import love.yinlin.compose.mutableRefStateOf
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.data.rachel.activity.Activity
import love.yinlin.extension.findAssign
import love.yinlin.compose.ui.layout.ActionScope
import love.yinlin.screen.common.ScreenMain
import love.yinlin.screen.msg.SubScreenMsg
import love.yinlin.compose.ui.floating.FloatingDialogCrop
import love.yinlin.compose.ui.floating.FloatingDialogInput
import love.yinlin.compose.ui.image.ClickIcon
import love.yinlin.compose.ui.image.ImageAdder
import love.yinlin.compose.ui.image.ReplaceableImage
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.input.ClickText
import love.yinlin.compose.ui.input.DockedDatePicker
import love.yinlin.compose.ui.input.LoadingClickText
import love.yinlin.compose.ui.input.Switch
import love.yinlin.compose.ui.text.TextInput
import love.yinlin.compose.ui.text.TextInputState
import love.yinlin.data.rachel.activity.ActivityLink
import love.yinlin.data.rachel.activity.ActivityPrice
import love.yinlin.extension.*
import love.yinlin.screen.community.BoxText

@Stable
class ScreenModifyActivity(manager: ScreenManager, private val aid: Int) : Screen(manager) {
	private val activities = manager.get<ScreenMain>().get<SubScreenMsg>().activities
	private val activity = activities.find { it.aid == aid }!!
	private val initDate = activity.ts?.let { DateEx.Formatter.standardDate.parse(it) }

	private val inputShortTitle = TextInputState(activity.shortTitle ?: "")
	private val inputTitle = TextInputState(activity.title ?: "")
	private var date: LocalDate? by mutableRefStateOf(initDate)
	private val inputTimeInfo = TextInputState(activity.tsInfo ?: "")
	private val inputLocation = TextInputState(activity.location ?: "")
	private val inputContent = TextInputState(activity.content ?: "")
	private val price = activity.price.toMutableStateList()
	private val saleTime = activity.saleTime.toMutableStateList()
	private val lineup = activity.lineup.toMutableStateList()
	private val playlist = activity.playlist.toMutableStateList()
	private val inputShowstart = TextInputState(activity.link.showstart ?: "")
	private val inputDamai = TextInputState(activity.link.damai ?: "")
	private val inputMaoyan = TextInputState(activity.link.maoyan ?: "")
	private val inputLink = TextInputState(activity.link.link ?: "")
	private val inputQQGroupPhone = TextInputState(activity.link.qqGroupPhone ?: "")
	private val inputQQGroupLink = TextInputState(activity.link.qqGroupLink ?: "")
	private var hide: Boolean by mutableStateOf(activity.hide)

	private var photo by mutableRefStateOf(activity.photo)

	private val canSubmit by derivedStateOf {
        !inputShortTitle.overflow && !inputTitle.overflow && !inputTimeInfo.overflow && !inputLocation.overflow && !inputContent.overflow
				&& !inputShowstart.overflow && !inputDamai.overflow && !inputMaoyan.overflow && !inputLink.overflow
				&& !inputQQGroupPhone.overflow && !inputQQGroupLink.overflow
    }

	private suspend fun updateActivity() {
		val ts = date?.let { DateEx.Formatter.standardDate.format(it) }
		val tsInfo = inputTimeInfo.text
		val location = inputLocation.text
		val shortTitle = inputShortTitle.text
		val title = inputTitle.text
		val content = inputContent.text
		val link = ActivityLink(
			showstart = inputShowstart.text,
			damai = inputDamai.text,
			maoyan = inputMaoyan.text,
			link = inputLink.text,
			qqGroupPhone = inputQQGroupPhone.text,
			qqGroupLink = inputQQGroupLink.text,
		)
		val newActivity = Activity(
			aid = aid,
			ts = ts,
			tsInfo = tsInfo,
			location = location,
			shortTitle = shortTitle,
			title = title,
			content = content,
			price = price,
			saleTime = saleTime,
			lineup = lineup,
			link = link,
			playlist = playlist,
			hide = hide
		)
		ApiActivityUpdateActivityInfo.request(app.config.userToken, newActivity) {
			activities.findAssign(predicate = { it.aid == aid }) {
				it.copy(
					ts = ts,
					tsInfo = tsInfo,
					location = location,
					shortTitle = shortTitle,
					title = title,
					content = content,
					price = price,
					saleTime = saleTime,
					lineup = lineup,
					link = link,
					playlist = playlist
				)
			}
			pop()
		}.errorTip
	}

	private suspend fun updatePhoto(key: String, path: Path) {
		slot.loading.openSuspend()
		catching {
			ApiActivityUpdateActivityPhoto.request(app.config.userToken, aid, key, apiFile(path)) { newPic ->
				activities.findAssign(predicate = { it.aid == aid }) {
					photo = photo.toJson().Object.toMutableMap().let { map ->
						map[key] = newPic.json
						map.toJson().to()
					}
					it.copy(photo = photo)
				}
			}.errorTip
		}
		slot.loading.close()
	}

	private suspend fun deletePhoto(key: String) {
		slot.loading.openSuspend()
		ApiActivityDeleteActivityPhoto.request(app.config.userToken, aid, key) {
			activities.findAssign(predicate = { it.aid == aid }) {
				photo = photo.toJson().Object.toMutableMap().let { map ->
					map.remove(key)
					map.toJson().to()
				}
				it.copy(photo = photo)
			}
		}.errorTip
		slot.loading.close()
	}

	private suspend fun addPhotos(key: String, files: List<Path>) {
		slot.loading.openSuspend()
		catching {
			ApiActivityAddActivityPhotos.request(app.config.userToken, aid, key, apiFile(files)!!) { newPics ->
				activities.findAssign(predicate = { it.aid == aid }) { oldActivity ->
					photo = photo.toJson().Object.toMutableMap().let { map ->
						map[key] = map[key].ArrayEmpty.toMutableList().also { list ->
							list += newPics.map { it.json }
						}.toJson()
						map.toJson().to()
					}
					oldActivity.copy(photo = photo)
				}
			}.errorTip
		}
		slot.loading.close()
	}

	private suspend fun updatePhotos(key: String, index: Int) {
		app.picker.pickPicture()?.use { source ->
			app.os.storage.createTempFile { sink ->
				val image = PlatformImage.decode(source.readByteArray())!!
				image.thumbnail()
				sink.write(image.encode(quality = ImageQuality.High)!!)
				true
			}
		}?.let { path ->
			slot.loading.openSuspend()
			catching {
				ApiActivityUpdateActivityPhotos.request(app.config.userToken, aid, key, index, apiFile(path)) { newPic ->
					activities.findAssign(predicate = { it.aid == aid }) { oldActivity ->
						photo = photo.toJson().Object.toMutableMap().let { map ->
							map[key] = map[key].Array.toMutableList().also { list ->
								list[index] = newPic.json
							}.toJson()
							map.toJson().to()
						}
						oldActivity.copy(photo = photo)
					}
				}.errorTip
			}
			slot.loading.close()
		}
	}

	private suspend fun deletePhotos(key: String, index: Int) {
		slot.loading.openSuspend()
		ApiActivityDeleteActivityPhotos.request(app.config.userToken, aid, key, index) {
			activities.findAssign(predicate = { it.aid == aid }) { oldActivity ->
				photo = photo.toJson().Object.toMutableMap().let { map ->
					map[key] = map[key].Array.toMutableList().also { list ->
						list.removeAt(index)
					}.toJson()
					map.toJson().to()
				}
				oldActivity.copy(photo = photo)
			}
		}.errorTip
		slot.loading.close()
	}

	suspend fun pickPicture(onPicAdd: suspend (Path) -> Unit) {
		val path = app.picker.pickPicture()?.use { source ->
			app.os.storage.createTempFile { sink -> source.transferTo(sink) > 0L }
		}
		if (path != null) {
			cropDialog.openSuspend(url = path.toString(), aspectRatio = 2f)?.let { rect ->
				app.os.storage.createTempFile { sink ->
					val image = PlatformImage.decode(path.readByteArray()!!)!!
					image.crop(rect)
					image.thumbnail()
					sink.write(image.encode(quality = ImageQuality.High)!!)
					true
				}?.let { onPicAdd(it) }
			}
		}
	}

	suspend fun pickPictures(currentSize: Int, onPicsAdd: suspend (List<Path>) -> Unit) {
		app.picker.pickPicture((9 - currentSize).coerceAtLeast(1))?.use { sources ->
			val path = mutableListOf<Path>()
			for (source in sources) {
				app.os.storage.createTempFile { sink ->
					val image = PlatformImage.decode(source.readByteArray())!!
					image.thumbnail()
					sink.write(image.encode(quality = ImageQuality.High)!!)
					true
				}?.let { path += it }
			}
			if (path.isNotEmpty()) onPicsAdd(path)
		}
	}

	override val title: String = "修改活动"

	@Composable
	override fun ActionScope.RightActions() {
		ActionSuspend(
			icon = Icons.Outlined.Check,
            tip = "提交",
			enabled = canSubmit
		) {
			updateActivity()
		}
	}

	@Composable
	override fun Content(device: Device) {
		Column(modifier = Modifier
			.padding(LocalImmersivePadding.current)
			.fillMaxSize()
			.padding(CustomTheme.padding.equalValue)
			.verticalScroll(rememberScrollState()),
			verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace),
		) {
			TextInput(
				state = inputShortTitle,
				hint = "短活动名称(可空, 5字)",
				maxLength = 5,
				imeAction = ImeAction.Next,
				modifier = Modifier.fillMaxWidth()
			)
			TextInput(
				state = inputTitle,
				hint = "活动名称(可空, 32字)",
				maxLength = 32,
				imeAction = ImeAction.Next,
				modifier = Modifier.fillMaxWidth()
			)
			DockedDatePicker(
				hint = "活动时间(可空)",
				initDate = initDate,
				onDateSelected = { date = it },
				modifier = Modifier.fillMaxWidth()
			)
			TextInput(
				state = inputTimeInfo,
				hint = "活动时间补充信息(可空, 128字)",
				maxLength = 128,
				imeAction = ImeAction.Next,
				modifier = Modifier.fillMaxWidth()
			)
			TextInput(
				state = inputLocation,
				hint = "活动地点(可空, 32字)",
				maxLength = 32,
				imeAction = ImeAction.Next,
				modifier = Modifier.fillMaxWidth()
			)
			TextInput(
				state = inputContent,
				hint = "活动内容(可空, 512字)",
				maxLength = 512,
				maxLines = 5,
				imeAction = ImeAction.Next,
				modifier = Modifier.fillMaxWidth()
			)
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalSpace)
			) {
				Text(text = "隐藏活动")
				Switch(
					checked = hide,
					onCheckedChange = { hide = it }
				)
			}
			ClickText(
				text = "添加票价",
				color = MaterialTheme.colorScheme.tertiary,
				icon = Icons.Outlined.MonetizationOn,
				onClick = {
					price += ActivityPrice(name = "新票种", value = 0)
				}
			)
			FlowRow(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalSpace)
			) {
				for ((index, item) in price.withIndex()) {
					Surface {
						Row(modifier = Modifier.padding(CustomTheme.padding.value)) {
							BoxText(
								text = item.name,
								color = MaterialTheme.colorScheme.primary,
								onClick = {
									launch {
										inputDialog.openSuspend()?.let { input ->
											price[index] = item.copy(name = input)
										}
									}
								}
							)
							BoxText(
								text = item.value.toString(),
								color = MaterialTheme.colorScheme.secondary,
								onClick = {
									launch {
										inputDialog.openSuspend()?.let { input ->
											val value = input.toIntOrNull()
											if (value == null || value < 0) slot.tip.warning("票价非法")
											else price[index] = item.copy(value = value)
										}
									}
								}
							)
							ClickIcon(
								icon = Icons.Outlined.Close,
								color = MaterialTheme.colorScheme.onSurface,
								onClick = { price.removeAt(index) }
							)
						}
					}
				}
			}
			LoadingClickText(
				text = "添加开售时间",
				icon = Icons.Outlined.Timer,
				color = MaterialTheme.colorScheme.tertiary,
				onClick = {
					inputDialog.openSuspend(DateEx.TodayString)?.let { input ->
						if (DateEx.Formatter.standardDate.parse(input) != null) saleTime += input
						else slot.tip.warning("开售时间格式非法")
					}
				}
			)
			FlowRow(modifier = Modifier.fillMaxWidth()) {
				for (item in saleTime) {
					BoxText(
						text = item,
						color = MaterialTheme.colorScheme.primary,
						onClick = { saleTime.remove(item) }
					)
				}
			}
			LoadingClickText(
				text = "添加阵容艺人",
				icon = Icons.Outlined.People,
				color = MaterialTheme.colorScheme.tertiary,
				onClick = {
					inputDialog.openSuspend()?.let { input ->
						lineup += input
					}
				}
			)
			FlowRow(modifier = Modifier.fillMaxWidth()) {
				for (item in lineup) {
					BoxText(
						text = item,
						color = MaterialTheme.colorScheme.primary,
						onClick = { lineup.remove(item) }
					)
				}
			}
			LoadingClickText(
				text = "添加歌单曲目",
				icon = Icons.Outlined.MusicNote,
				color = MaterialTheme.colorScheme.tertiary,
				onClick = {
					inputDialog.openSuspend()?.let { input ->
						playlist += input
					}
				}
			)
			FlowRow(modifier = Modifier.fillMaxWidth()) {
				for (item in playlist) {
					BoxText(
						text = item,
						color = MaterialTheme.colorScheme.primary,
						onClick = { playlist.remove(item) }
					)
				}
			}
			TextInput(
				state = inputShowstart,
				hint = "秀动ID(可空)",
				maxLength = 1024,
				imeAction = ImeAction.Next,
				modifier = Modifier.fillMaxWidth()
			)
			TextInput(
				state = inputDamai,
				hint = "大麦ID(可空)",
				maxLength = 16,
				imeAction = ImeAction.Next,
				modifier = Modifier.fillMaxWidth()
			)
			TextInput(
				state = inputMaoyan,
				hint = "猫眼ID(可空)",
				maxLength = 16,
				imeAction = ImeAction.Next,
				modifier = Modifier.fillMaxWidth()
			)
			TextInput(
				state = inputLink,
				hint = "活动链接(可空)",
				maxLength = 256,
				modifier = Modifier.fillMaxWidth()
			)
			TextInput(
				state = inputQQGroupPhone,
				hint = "QQ群号(可空, 仅手机端)",
				maxLength = 16,
				modifier = Modifier.fillMaxWidth()
			)
			TextInput(
				state = inputQQGroupLink,
				hint = "QQ群分享链接(可空, 非手机端)",
				maxLength = 128,
				modifier = Modifier.fillMaxWidth()
			)
			Text(
				text = "轮播图(可空)",
				style = MaterialTheme.typography.titleMedium
			)
			ReplaceableImage(
				pic = remember(photo) { photo.coverPath?.url },
				modifier = Modifier.fillMaxWidth().aspectRatio(2f),
				onReplace = {
					launch {
						pickPicture {
							updatePhoto("cover", it)
						}
					}
				},
				onDelete = {
					launch { deletePhoto("cover") }
				}
			) { uri ->
				WebImage(
					uri = uri,
					contentScale = ContentScale.Crop,
					modifier = Modifier.fillMaxSize()
				)
			}
			Text(
				text = "座位图(可空)",
				style = MaterialTheme.typography.titleMedium
			)
			ReplaceableImage(
				pic = remember(photo) { photo.seatPath?.url },
				modifier = Modifier.fillMaxWidth().aspectRatio(2f),
				onReplace = {
					launch {
						pickPicture {
							updatePhoto("seat", it)
						}
					}
				},
				onDelete = {
					launch { deletePhoto("seat") }
				}
			) { uri ->
				WebImage(
					uri = uri,
					contentScale = ContentScale.Crop,
					modifier = Modifier.fillMaxSize()
				)
			}
			Text(
				text = "活动海报",
				style = MaterialTheme.typography.titleMedium
			)
			ImageAdder(
				maxNum = 9,
				pics = photo.posters,
				size = CustomTheme.size.microCellWidth,
				modifier = Modifier.fillMaxWidth(),
				onAdd = {
					launch {
						pickPictures(photo.posters.size) {
							addPhotos("posters", it)
						}
					}
				},
				onDelete = {
					launch { deletePhotos("posters", it) }
				},
				onClick = { index, _ ->
					launch { updatePhotos("posters", index) }
				}
			) { _, pic ->
				WebImage(
					uri = photo.posterPath(pic).url,
					contentScale = ContentScale.Crop,
					modifier = Modifier.fillMaxSize()
				)
			}
		}
	}

	private val cropDialog = this land FloatingDialogCrop()
	private val inputDialog = this land FloatingDialogInput(hint = "修改信息", maxLength = 64)
}