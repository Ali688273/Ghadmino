package ir.ghadmino.stepcounter.stats

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.ghadmino.stepcounter.ads.GhadminoBanner
import ir.ghadmino.stepcounter.step.StepHistory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.max

private data class Point(val label:String,val value:Int)

@Composable
fun StatsScreen(stats:GhadminoStats, goal:Int){
    val context=LocalContext.current
    var period by remember{mutableIntStateOf(7)}
    var type by remember{mutableStateOf("line")}
    val data=remember(period){chartData(context,period)}
    val report=remember(period){StatsRepository.period(context,goal,period)}
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.55f))){
        LazyColumn(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(14.dp)){
            item{
                Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer)){
                    Column(Modifier.padding(18.dp)){
                        Text("آمار و گزارش فعالیت",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
                        Text("گزارش هفتگی، ماهانه و سالانه با نمودار قابل انتخاب.")
                    }
                }
            }
            item{GhadminoBanner(Modifier.fillMaxWidth())}
            item{PeriodSelector(period){period=it}}
            item{
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    Metric(Modifier.weight(1f),"مجموع",report.totalSteps.toString(),"قدم")
                    Metric(Modifier.weight(1f),"میانگین",report.averageSteps.toString(),"قدم/روز")
                    Metric(Modifier.weight(1f),"موفق",report.goalDays.toString(),"روز")
                }
            }
            item{
                Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.secondaryContainer)){
                    Column(Modifier.padding(16.dp)){
                        Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.BarChart,null);Spacer(Modifier.width(8.dp));Text("نمودار فعالیت",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)}
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                            FilterChip(selected=type=="line",onClick={type="line"},label={Text("خطی")})
                            FilterChip(selected=type=="bar",onClick={type="bar"},label={Text("میله‌ای")})
                        }
                        Spacer(Modifier.height(10.dp))
                        ActivityChart(data,type=="line",goal)
                    }
                }
            }
            item{
                Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.tertiaryContainer)){
                    Column(Modifier.padding(16.dp)){
                        Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.TrendingUp,null);Spacer(Modifier.width(8.dp));Text("روند و پیشرفت",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)}
                        val trend=StatsRepository.trend(context,goal)
                        Text(if(trend>0)"این هفته نسبت به هفته قبل "+trend+"٪ بیشتر بوده است." else if(trend<0)"این هفته نسبت به هفته قبل "+(-trend)+"٪ کمتر بوده است." else "تغییر محسوسی نسبت به هفته قبل ثبت نشده است.")
                        Text("هدف روزانه: "+goal+" قدم")
                        Text("زنجیره فعلی: "+stats.streak+" روز • طولانی‌ترین: "+stats.longestStreak.value+" روز")
                    }
                }
            }
            item{
                Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp)){
                    Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.EmojiEvents,null);Spacer(Modifier.width(8.dp));Text("رکوردها",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)}
                    Text("بهترین روز: "+StatsRepository.label(stats.bestDay.first)+" — "+stats.bestDay.second+" قدم")
                    Text("رکورد زنجیره: "+stats.longestStreak.value+" روز")
                }}
            }
            item{
                Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp)){
                    Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.CalendarMonth,null);Spacer(Modifier.width(8.dp));Text("۷ روز اخیر",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)}
                    val recent=StepHistory.recent(context,7)
                    recent.forEach{r->Row(Modifier.fillMaxWidth().padding(vertical=7.dp),horizontalArrangement=Arrangement.SpaceBetween){Text(StatsRepository.label(r.first));Text(r.second.toString()+" قدم",fontWeight=FontWeight.Bold)};HorizontalDivider()}
                }}
            }
            item{
                Card(Modifier.fillMaxWidth()){Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.Route,null);Spacer(Modifier.width(8.dp));Text("مسافت و کالری تخمینی‌اند و بر اساس پروفایل و فعالیت ثبت‌شده محاسبه می‌شوند.")}}
            }
        }
    }
}

@Composable private fun PeriodSelector(current:Int,onSelected:(Int)->Unit){
    Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp)){
        Text("بازه گزارش",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
            FilterChip(selected=current==7,onClick={onSelected(7)},label={Text("هفتگی")})
            FilterChip(selected=current==30,onClick={onSelected(30)},label={Text("ماهانه")})
            FilterChip(selected=current==365,onClick={onSelected(365)},label={Text("سالانه")})
        }
    }}
}

@Composable private fun ActivityChart(data:List<Point>,line:Boolean,goal:Int){
    if(data.isEmpty()){Box(Modifier.fillMaxWidth().height(220.dp),contentAlignment=Alignment.Center){Text("هنوز داده‌ای ثبت نشده است.")};return}
    val maxValue=max(1,max(data.maxOfOrNull{it.value}?:0,goal))
    Column{
        Canvas(Modifier.fillMaxWidth().height(220.dp)){
            val left=14f;val right=size.width-10f;val top=12f;val bottom=size.height-18f;val w=right-left;val h=bottom-top
            for(i in 0..4){val y=top+h*i/4f;drawLine(MaterialTheme.colorScheme.outline.copy(alpha=.16f),Offset(left,y),Offset(right,y),1f)}
            val gy=bottom-h*(goal.toFloat()/maxValue).coerceIn(0f,1f)
            drawLine(MaterialTheme.colorScheme.error.copy(alpha=.55f),Offset(left,gy),Offset(right,gy),2f)
            if(line){
                val p=Path()
                data.forEachIndexed{i,v->{val x=if(data.size==1)left else left+w*i/(data.size-1);val y=bottom-h*v.value.toFloat()/maxValue;if(i==0)p.moveTo(x,y)else p.lineTo(x,y)}}
                drawPath(p,MaterialTheme.colorScheme.primary,Stroke(5f,cap=StrokeCap.Round))
                data.forEachIndexed{i,v->{val x=if(data.size==1)left else left+w*i/(data.size-1);val y=bottom-h*v.value.toFloat()/maxValue;drawCircle(MaterialTheme.colorScheme.primary,5f,Offset(x,y))}}
            }else{
                val bw=((w-4f*(data.size-1))/data.size).coerceIn(3f,30f)
                data.forEachIndexed{i,v->{val x=left+i*(bw+4f);val bh=h*v.value.toFloat()/maxValue;drawRoundRect(MaterialTheme.colorScheme.primary,Offset(x,bottom-bh),androidx.compose.ui.geometry.Size(bw,bh),CornerRadius(bw/2,bw/2))}}
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.SpaceBetween){
            data.forEach{Text(it.label,style=MaterialTheme.typography.labelSmall,modifier=Modifier.padding(horizontal=4.dp))}
        }
    }
}

private fun chartData(c:Context,period:Int):List<Point>{
    val rows=StepHistory.recent(c,period).associate{it.first to it.second}
    return if(period<365){
        val key=SimpleDateFormat("yyyy-MM-dd",Locale.US);val label=SimpleDateFormat("MM/dd",Locale.US)
        (period-1 downTo 0).map{off->val d=Calendar.getInstance().apply{add(Calendar.DAY_OF_YEAR,-off)}.time;Point(label.format(d),rows[key.format(d)]?:0)}
    }else{
        val key=SimpleDateFormat("yyyy-MM",Locale.US);val label=SimpleDateFormat("MM",Locale.US)
        (11 downTo 0).map{off->val d=Calendar.getInstance().apply{set(Calendar.DAY_OF_MONTH,1);add(Calendar.MONTH,-off)}.time;val prefix=key.format(d);Point(label.format(d),rows.filterKeys{it.startsWith(prefix)}.values.sum())}
    }
}

@Composable private fun Metric(modifier:Modifier,title:String,value:String,unit:String){
    Card(modifier){Column(Modifier.padding(12.dp)){Text(title,style=MaterialTheme.typography.labelMedium);Text(value,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);Text(unit,style=MaterialTheme.typography.labelSmall)}}
}
