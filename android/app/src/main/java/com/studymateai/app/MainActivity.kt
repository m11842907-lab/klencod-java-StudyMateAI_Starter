package com.studymateai.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { StudyMateScreen() }
    }
}

private val Purple = Color(0xFF6D5CE8)
private val Ink = Color(0xFF202039)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyMateScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedName by remember { mutableStateOf<String?>(null) }
    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            selectedUri = uri
            selectedName = queryDisplayName(context, uri) ?: "lecture.pdf"
            answer = ""
            error = ""
        }
    }

    MaterialTheme(
        colorScheme = lightColorScheme(primary = Purple, secondary = Color(0xFF00A99A), background = Color(0xFFF6F5FC))
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("StudyMate AI", fontWeight = FontWeight.Bold, color = Ink)
                            Text("مساعدك الذكي في الجامعة", fontSize = 12.sp, color = Color.Gray)
                        }
                    },
                    navigationIcon = {
                        Icon(Icons.Default.MenuBook, contentDescription = null, tint = Purple,
                            modifier = Modifier.padding(start = 16.dp, end = 8.dp).size(28.dp))
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(colors = CardDefaults.cardColors(containerColor = Ink), shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFC7BEFF), modifier = Modifier.size(32.dp))
                        Text("ذاكر بذكاء، مش بس بوقت أطول ✨", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text("ارفع محاضرتك واسأل عنها أو اطلب ملخصًا منظمًا.", color = Color(0xFFE0DDF5))
                        Button(onClick = { picker.launch(arrayOf("application/pdf")) }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.AttachFile, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("اختار ملف المحاضرة PDF")
                        }
                    }
                }

                if (selectedName != null) {
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MenuBook, contentDescription = null, tint = Purple)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("الملف المختار", fontWeight = FontWeight.SemiBold, color = Ink)
                                Text(selectedName!!, color = Color.Gray, fontSize = 13.sp)
                            }
                            TextButton(onClick = { selectedUri = null; selectedName = null; answer = "" }) { Text("إزالة") }
                        }
                    }
                }

                Text("اسأل عن محاضرتك", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("مثال: اشرح أهم المفاهيم ببساطة") },
                    minLines = 3,
                    shape = RoundedCornerShape(16.dp)
                )

                Button(
                    onClick = {
                        val uri = selectedUri
                        if (uri == null) { error = "اختار ملف PDF الأول."; return@Button }
                        if (question.isBlank()) { error = "اكتب سؤالك عن المحاضرة."; return@Button }
                        scope.launch {
                            loading = true; error = ""; answer = ""
                            try {
                                answer = withContext(Dispatchers.IO) {
                                    uploadAndAsk(context, uri, selectedName ?: "lecture.pdf", "ask", question)
                                }
                            } catch (e: Exception) { error = e.message ?: "حصل خطأ أثناء الاتصال." }
                            finally { loading = false }
                        }
                    },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                    else { Icon(Icons.Default.Send, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("اسأل الذكاء الاصطناعي") }
                }

                OutlinedButton(
                    onClick = {
                        val uri = selectedUri
                        if (uri == null) { error = "اختار ملف PDF الأول."; return@OutlinedButton }
                        scope.launch {
                            loading = true; error = ""; answer = ""
                            try {
                                answer = withContext(Dispatchers.IO) {
                                    uploadAndAsk(context, uri, selectedName ?: "lecture.pdf", "summarize", "")
                                }
                            } catch (e: Exception) { error = e.message ?: "حصل خطأ أثناء الاتصال." }
                            finally { loading = false }
                        }
                    },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("✨ لخّص المحاضرة") }

                if (error.isNotBlank()) {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))) {
                        Text(error, color = Color(0xFFB3261E), modifier = Modifier.padding(14.dp))
                    }
                }
                if (answer.isNotBlank()) {
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("إجابة StudyMate", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Purple)
                            Text(answer, color = Ink, lineHeight = 24.sp)
                        }
                    }
                }
                Text(
                    "ملاحظة: النسخة الأولية تدعم ملفات PDF النصية. ملفات PDF الممسوحة ضوئيًا تحتاج OCR في إصدار لاحق.",
                    color = Color.Gray, fontSize = 12.sp
                )
            }
        }
    }
}

private fun queryDisplayName(context: android.content.Context, uri: Uri): String? {
    return context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
}

private fun uploadAndAsk(
    context: android.content.Context,
    uri: Uri,
    filename: String,
    mode: String,
    question: String
): String {
    val input = context.contentResolver.openInputStream(uri) ?: error("تعذّر فتح الملف.")
    val temp = File.createTempFile("lecture_", ".pdf", context.cacheDir)
    input.use { source -> temp.outputStream().use { target -> source.copyTo(target) } }

    try {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .callTimeout(150, TimeUnit.SECONDS)
            .build()

        val pdfBody = temp.asRequestBody("application/pdf".toMediaType())
        val multipart = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("file", filename, pdfBody)
            .addFormDataPart("mode", mode)
            .addFormDataPart("question", question)
            .build()

        val request = Request.Builder()
            .url(ApiConfig.BASE_URL + "api/study")
            .post(multipart)
            .build()

        client.newCall(request).execute().use { response ->
            val bodyText = response.body?.string().orEmpty()
            val json = try { JSONObject(bodyText) } catch (_: Exception) { JSONObject() }
            if (!response.isSuccessful) throw IllegalStateException(json.optString("error", "خطأ من الخادم (${response.code})"))
            return json.optString("answer", "لم تصل إجابة من الخادم.")
        }
    } finally {
        temp.delete()
    }
}
