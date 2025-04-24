package love.yinlin.ui.screen.community

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.api.API
import love.yinlin.api.ClientAPI
import love.yinlin.data.Data
import love.yinlin.data.rachel.mail.Mail
import love.yinlin.extension.findAssign
import love.yinlin.platform.app
import love.yinlin.ui.component.input.RachelButton
import love.yinlin.ui.component.layout.BoxState
import love.yinlin.ui.component.layout.PaginationArgs
import love.yinlin.ui.component.layout.PaginationGrid
import love.yinlin.ui.component.layout.StatefulBox
import love.yinlin.ui.component.screen.Sheet
import love.yinlin.ui.component.screen.SheetState
import love.yinlin.ui.component.screen.SubScreen
import love.yinlin.ui.screen.Screen

@Stable
class ScreenMail(model: AppModel) : Screen<ScreenMail.Args>(model) {
	@Stable
	@Serializable
	data object Args : Screen.Args

	private var state by mutableStateOf(BoxState.EMPTY)

	private val page = object : PaginationArgs<Mail, Long, Boolean>(Long.MAX_VALUE, false) {
		override fun offset(item: Mail): Long = item.mid
		override fun arg1(item: Mail): Boolean = item.processed
	}

	private val mailDetailsSheet = SheetState<Mail>()

	private suspend fun requestNewMails() {
		if (state != BoxState.LOADING) {
			state = BoxState.LOADING
			val result = ClientAPI.request(
				route = API.User.Mail.GetMails,
				data = API.User.Mail.GetMails.Request(
					token = app.config.userToken,
					num = page.pageNum
				)
			)
			state = if (result is Data.Success) {
				if (page.newData(result.data)) BoxState.CONTENT else BoxState.EMPTY
			} else BoxState.NETWORK_ERROR
		}
	}

	private suspend fun requestMoreMails() {
		val result = ClientAPI.request(
			route = API.User.Mail.GetMails,
			data = API.User.Mail.GetMails.Request(
				token = app.config.userToken,
				isProcessed = page.arg1,
				offset = page.offset,
				num = page.pageNum
			)
		)
		if (result is Data.Success) page.moreData(result.data)
	}

	private suspend fun onProcessMail(text: String, mid: Long, value: Boolean) {
		if (slot.confirm.open(content = text)) {
			slot.loading.open()
			val result = ClientAPI.request(
				route = API.User.Mail.ProcessMail,
				data = API.User.Mail.ProcessMail.Request(
					token = app.config.userToken,
					mid = mid,
					confirm = value
				)
			)
			when (result) {
				is Data.Success -> {
					page.items.findAssign(predicate = { it.mid == mid }) {
						it.copy(processed = true)
					}
					mailDetailsSheet.hide()
				}
				is Data.Error -> slot.tip.error(result.message)
			}
			slot.loading.hide()
		}
	}

	private suspend fun onDeleteMail(mid: Long) {
		if (slot.confirm.open(content = "删除此邮件?")) {
			slot.loading.open()
			val result = ClientAPI.request(
				route = API.User.Mail.DeleteMail,
				data = API.User.Mail.DeleteMail.Request(
					token = app.config.userToken,
					mid = mid
				)
			)
			when (result) {
				is Data.Success -> {
					page.items.removeAll { it.mid == mid }
					mailDetailsSheet.hide()
				}
				is Data.Error -> slot.tip.error(result.message)
			}
			slot.loading.hide()
		}
	}

	@Composable
	private fun MailItem(
		mail: Mail,
		modifier: Modifier = Modifier
	) {
		Surface(
			modifier = modifier,
			shadowElevation = 3.dp,
			border = if (mail.processed) null else BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
		) {
			Column(
				modifier = Modifier.fillMaxWidth().clickable{ mailDetailsSheet.open(mail) }.padding(10.dp),
				horizontalAlignment = Alignment.CenterHorizontally,
				verticalArrangement = Arrangement.spacedBy(10.dp)
			) {
				Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.spacedBy(10.dp),
					verticalAlignment = Alignment.CenterVertically
				) {
					BoxText(
						text = mail.typeString,
						color = when (mail.type) {
							Mail.Type.INFO -> MaterialTheme.colorScheme.primary
							Mail.Type.CONFIRM -> MaterialTheme.colorScheme.secondary
							Mail.Type.DECISION -> MaterialTheme.colorScheme.tertiary
							else -> LocalContentColor.current
						}
					)
					Text(
						text = mail.title,
						style = MaterialTheme.typography.titleMedium,
						maxLines = 1,
						overflow = TextOverflow.Ellipsis,
						modifier = Modifier.weight(1f)
					)
					Text(
						text = mail.ts,
						color = MaterialTheme.colorScheme.onSurfaceVariant,
						style = MaterialTheme.typography.bodyMedium
					)
				}
				Text(
					text = mail.content,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
					maxLines = 1,
					overflow = TextOverflow.Ellipsis,
					modifier = Modifier.fillMaxWidth()
				)
			}
		}
	}

	@Composable
	private fun MailDetailsLayout(mail: Mail) {
		Sheet(
			state = mailDetailsSheet,
			heightModifier = { heightIn(min = 200.dp, max = 500.dp) }
		) {
			Column(
				modifier = Modifier.fillMaxWidth().padding(10.dp)
					.verticalScroll(rememberScrollState()),
				verticalArrangement = Arrangement.spacedBy(10.dp)
			) {
				Text(
					text = mail.title,
					style = MaterialTheme.typography.titleLarge,
					textAlign = TextAlign.Center,
					maxLines = 1,
					overflow = TextOverflow.Ellipsis,
					modifier = Modifier.fillMaxWidth()
				)
				Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
					verticalAlignment = Alignment.CenterVertically
				) {
					if (mail.withYes) RachelButton(
						text = "接受",
						icon = Icons.Outlined.CheckCircle,
						onClick = {
							launch { onProcessMail("接受此邮件结果?", mail.mid, true) }
						}
					)
					if (mail.withNo) RachelButton(
						text = "拒绝",
						icon = Icons.Outlined.Cancel,
						color = MaterialTheme.colorScheme.error,
						onClick = {
							launch { onProcessMail("拒绝此邮件结果?", mail.mid, false) }
						}
					)
					if (mail.processed) RachelButton(
						text = "删除",
						icon = Icons.Outlined.Delete,
						color = MaterialTheme.colorScheme.secondary,
						onClick = {
							launch { onDeleteMail(mail.mid) }
						}
					)
				}
				Text(
					text = mail.content,
					style = MaterialTheme.typography.bodyMedium,
					modifier = Modifier.fillMaxWidth()
				)
			}
		}
	}

	override suspend fun initialize() {
		requestNewMails()
	}

	@Composable
	override fun Content() {
		SubScreen(
			modifier = Modifier.fillMaxSize(),
			title = "邮箱",
			onBack = { pop() },
			slot = slot
		) {
			StatefulBox(
				state = state,
				modifier = Modifier.fillMaxSize()
			) {
				PaginationGrid(
					items = page.items,
					key = { it.mid },
					columns = GridCells.Adaptive(300.dp),
					canRefresh = true,
					canLoading = page.canLoading,
					onRefresh = { requestNewMails() },
					onLoading = { requestMoreMails() },
					modifier = Modifier.fillMaxSize(),
					contentPadding = PaddingValues(10.dp),
					horizontalArrangement = Arrangement.spacedBy(10.dp),
					verticalArrangement = Arrangement.spacedBy(10.dp)
				) {
					MailItem(mail = it)
				}
			}
		}

		mailDetailsSheet.WithOpen { MailDetailsLayout(it) }
	}
}