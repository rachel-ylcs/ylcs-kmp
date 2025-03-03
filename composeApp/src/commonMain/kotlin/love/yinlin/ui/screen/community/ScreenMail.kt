package love.yinlin.ui.screen.community

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import love.yinlin.AppModel
import love.yinlin.ThemeColor
import love.yinlin.api.API
import love.yinlin.api.ClientAPI
import love.yinlin.data.Data
import love.yinlin.data.rachel.Mail
import love.yinlin.extension.launchFlag
import love.yinlin.extension.LaunchOnce
import love.yinlin.extension.replaceAll
import love.yinlin.platform.config
import love.yinlin.ui.component.layout.BoxState
import love.yinlin.ui.component.layout.PaginationGrid
import love.yinlin.ui.component.layout.StatefulBox
import love.yinlin.ui.component.screen.SubScreen

private class MailModel(private val model: AppModel) : ViewModel() {
	val flagFirstLoad = launchFlag()
	var state by mutableStateOf(BoxState.EMPTY)

	val items = mutableStateListOf<Mail>()
	var offset: Long = Long.MAX_VALUE
	var canLoading by mutableStateOf(false)

	suspend fun requestNewMails() {
		if (state != BoxState.LOADING) {
			state = BoxState.LOADING
			val result = ClientAPI.request(
				route = API.User.Mail.GetMails,
				data = API.User.Mail.GetMails.Request(token = config.userToken)
			)
			if (result is Data.Success) {
				val data = result.data
				items.replaceAll(data)
				offset = data.lastOrNull()?.mid ?: Long.MAX_VALUE
				state = if (data.isEmpty()) BoxState.EMPTY else BoxState.CONTENT
				canLoading = offset != Long.MAX_VALUE
			}
			else state = BoxState.NETWORK_ERROR
		}
	}

	suspend fun requestMoreMails() {
		val result = ClientAPI.request(
			route = API.User.Mail.GetMails,
			data = API.User.Mail.GetMails.Request(
				token = config.userToken,
				offset = offset
			)
		)
		if (result is Data.Success) {
			val data = result.data
			items += data
			offset = data.lastOrNull()?.mid ?: Long.MAX_VALUE
			canLoading = offset != Long.MAX_VALUE
		}
	}

	fun onMailClick(mail: Mail) {

	}
}

@Composable
private fun MailItem(
	mail: Mail,
	modifier: Modifier = Modifier,
	onClick: () -> Unit
) {
	Surface(
		modifier = modifier,
		shadowElevation = 3.dp
	) {
		Column(
			modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(10.dp),
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
						else -> MaterialTheme.colorScheme.onSurface
					}
				)
				Text(
					text = mail.title,
					style = MaterialTheme.typography.titleLarge,
					maxLines = 1,
					overflow = TextOverflow.Ellipsis,
					modifier = Modifier.weight(1f)
				)
				Text(
					text = mail.ts,
					color = ThemeColor.fade,
					style = MaterialTheme.typography.bodyMedium
				)
			}
			Text(
				text = mail.content,
				color = ThemeColor.fade,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis,
				modifier = Modifier.fillMaxWidth()
			)
		}
	}
}

@Composable
fun ScreenMail(model: AppModel) {
	val screenModel = viewModel { MailModel(model) }

	SubScreen(
		modifier = Modifier.fillMaxSize(),
		title = "邮箱",
		onBack = { model.pop() }
	) {
		StatefulBox(
			state = screenModel.state,
			modifier = Modifier.fillMaxSize()
		) {
			PaginationGrid(
				items = screenModel.items,
				key = { it.mid },
				columns = GridCells.Adaptive(300.dp),
				canRefresh = true,
				canLoading = screenModel.canLoading,
				onRefresh = { screenModel.requestNewMails() },
				onLoading = { screenModel.requestMoreMails() },
				modifier = Modifier.fillMaxSize(),
				contentPadding = PaddingValues(10.dp),
				horizontalArrangement = Arrangement.spacedBy(10.dp),
				verticalArrangement = Arrangement.spacedBy(10.dp)
			) {
				MailItem(
					mail = it,
					onClick = { screenModel.onMailClick(it) }
				)
			}
		}
	}

	LaunchOnce(screenModel.flagFirstLoad) {
		screenModel.requestNewMails()
	}
}