package ir.ghadmino.stepcounter.challenge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import ir.ghadmino.stepcounter.reward.CoinWallet
@Composable fun ChallengesScreen(onChanged:()->Unit={}){
 val c=LocalContext.current;var refresh by remember{mutableIntStateOf(0)};val balance=remember(refresh){CoinWallet.balance(c)}
 LazyColumn(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(8.dp)){
  item{Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer)){Column(Modifier.padding(18.dp)){Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.EmojiEvents,null);Spacer(Modifier.width(8.dp));Text("چالش‌ها",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)};Spacer(Modifier.height(6.dp));Text("با حفظ فعالیت روزانه، چالش‌ها را کامل کن و سکه بگیر.");Text("موجودی: "+balance+" سکه")}}}
  items(ChallengeRepository.challenges){ch->val p=ChallengeRepository.progress(c,ch);val claimed=ChallengeRepository.isClaimed(c,ch.id);Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp)){Row(verticalAlignment=Alignment.CenterVertically){Icon(if(claimed)Icons.Default.TaskAlt else Icons.Default.Lock,null);Spacer(Modifier.width(8.dp));Text(ch.title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)};Spacer(Modifier.height(6.dp));Text(ch.description);Spacer(Modifier.height(10.dp));LinearProgressIndicator(progress={(p.toFloat()/ch.days).coerceIn(0f,1f)},Modifier.fillMaxWidth());Spacer(Modifier.height(6.dp));Text(p.toString()+" از "+ch.days+" روز • پاداش "+ch.reward+" سکه");Spacer(Modifier.height(8.dp));Button(onClick={if(ChallengeRepository.claim(c,ch)){refresh++;onChanged()}},enabled=p>=ch.days&&!claimed,modifier=Modifier.fillMaxWidth()){Text(if(claimed)"پاداش دریافت شد" else if(p>=ch.days)"دریافت پاداش" else "در حال پیشرفت")}}}}
 }
}