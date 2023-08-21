package me.rhunk.snapenhance.ui.manager.sections.social

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import me.rhunk.snapenhance.RemoteSideContext
import me.rhunk.snapenhance.core.messaging.MessagingRuleType
import me.rhunk.snapenhance.core.messaging.SocialScope
import me.rhunk.snapenhance.ui.util.BitmojiImage
import me.rhunk.snapenhance.util.snap.BitmojiSelfie

class ScopeContent(
    private val context: RemoteSideContext,
    private val section: SocialSection,
    private val navController: NavController,
    private val scope: SocialScope,
    private val id: String
) {
    @Composable
    private fun DeleteScopeEntityButton() {
        val coroutineScope = rememberCoroutineScope()
        OutlinedButton(onClick = {
            when (scope) {
                SocialScope.FRIEND -> context.modDatabase.deleteFriend(id)
                SocialScope.GROUP -> context.modDatabase.deleteGroup(id)
            }
            context.modDatabase.executeAsync {
                coroutineScope.launch {
                    section.onResumed()
                    navController.navigate(SocialSection.MAIN_ROUTE)
                }
            }
        }) {
            Text(text = "Delete ${scope.key}")
        }
    }

    @Composable
    fun Content() {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
        ) {
            when (scope) {
                SocialScope.FRIEND -> Friend()
                SocialScope.GROUP -> Group()
            }

            Spacer(modifier = Modifier.height(16.dp))

            val scopeRules = context.modDatabase.getRulesFromId(scope, id)

            SectionTitle("Rules")

            ContentCard {
                //manager anti features etc
                MessagingRuleType.values().forEach { feature ->
                    var featureEnabled by remember {
                        mutableStateOf(scopeRules.any { it.subject == feature.key })
                    }
                    val featureEnabledText = if (featureEnabled) "Enabled" else "Disabled"

                    Row {
                        Text(text = "${feature.key}: $featureEnabledText", maxLines = 1)
                        Switch(checked = featureEnabled, onCheckedChange = {
                            context.modDatabase.toggleRuleFor(scope, id, feature.key, it)
                            featureEnabled = it
                        })
                    }
                }
            }
        }
    }

    @Composable
    private fun ContentCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
        Card(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(10.dp)
                    .fillMaxWidth()
                    .then(modifier)
            ) {
                content()
            }
        }
    }

    @Composable
    private fun SectionTitle(title: String) {
        Text(
            text = title,
            maxLines = 1,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .offset(x = 20.dp)
                .padding(bottom = 10.dp)
        )
    }

    //need to display all units?
    private fun computeStreakETA(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val stringBuilder = StringBuilder()
        val diff = timestamp - now
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        if (days > 0) {
            stringBuilder.append("$days days ")
            return stringBuilder.toString()
        }
        if (hours > 0) {
            stringBuilder.append("$hours hours ")
            return stringBuilder.toString()
        }
        if (minutes > 0) {
            stringBuilder.append("$minutes minutes ")
            return stringBuilder.toString()
        }
        if (seconds > 0) {
            stringBuilder.append("$seconds seconds ")
            return stringBuilder.toString()
        }
        return "Expired"
    }

    @Composable
    private fun Friend() {
        //fetch the friend from the database
        val friend = remember { context.modDatabase.getFriendInfo(id) } ?: run {
            Text(text = "Friend not found")
            return
        }

        val streaks = remember {
            context.modDatabase.getFriendStreaks(id)
        }

        Column(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val bitmojiUrl = (friend.selfieId to friend.bitmojiId).let { (selfieId, bitmojiId) ->
                if (selfieId == null || bitmojiId == null) return@let null
                BitmojiSelfie.getBitmojiSelfie(
                    selfieId,
                    bitmojiId,
                    BitmojiSelfie.BitmojiSelfieType.THREE_D
                )
            }
            BitmojiImage(context = context, url = bitmojiUrl, size = 100)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = friend.displayName ?: friend.mutableUsername,
                maxLines = 1,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = friend.mutableUsername,
                maxLines = 1,
                fontSize = 12.sp,
                fontWeight = FontWeight.Light
            )
            Spacer(modifier = Modifier.height(16.dp))

            DeleteScopeEntityButton()
        }

        Spacer(modifier = Modifier.height(16.dp))
        Column {
            //streaks
            streaks?.let {
                var shouldNotify by remember { mutableStateOf(it.notify) }
                SectionTitle("Streaks")
                ContentCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(text = "Count: ${streaks.length}", maxLines = 1)
                            Text(text = "Expires in: ${computeStreakETA(streaks.expirationTimestamp)}", maxLines = 1)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Notify Expiration", maxLines = 1)
                            Switch(checked = shouldNotify, onCheckedChange = {
                                context.modDatabase.setFriendStreaksNotify(id, it)
                                shouldNotify = it
                            })
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun Group() {
        //fetch the group from the database
        val group = remember { context.modDatabase.getGroupInfo(id) } ?: run {
            Text(text = "Group not found")
            return
        }

        Column {
            Text(text = group.name, maxLines = 1)
            Text(text = "participantsCount: ${group.participantsCount}", maxLines = 1)
            Spacer(modifier = Modifier.height(16.dp))
            DeleteScopeEntityButton()
        }
    }
}