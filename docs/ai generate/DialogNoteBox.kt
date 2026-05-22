import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ActivityNoteComponent(
    onLabelClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    _onContinueAddClick: () -> Unit = {},
    _onAddClick: () -> Unit = {}
) {
    var noteText by remember { mutableStateOf("") }
    val maxCharLimit = 5000

    // MD3 风格颜色定义
    val backgroundColor =  MaterialTheme.colorScheme.surface
    val primaryBlue = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(Color.Transparent)
    ) {
        // 第一行：右侧的标签按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                onClick = onLabelClick,
                colors = ButtonDefaults.buttonColors(containerColor = primaryBlue),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text("标签", color = MaterialTheme.colorScheme.onPrimary, fontSize = 14.sp)
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 第二行：输入框区域与右侧历史按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            // 输入框卡片
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(150.dp)
                    .background(backgroundColor, RoundedCornerShape(16.dp))
                    .padding(8.dp)
            ) {

                // 核心输入框
               BasicTextField(
    value = noteText,
    onValueChange = { if (it.length <= maxCharLimit) noteText = it },
    modifier = Modifier
        .fillMaxSize()
        .padding(top = 1.dp),
    textStyle = TextStyle(
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    ),
    decorationBox = { innerTextField ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // placeholder
            if (noteText.isEmpty()) {
                Text(
                    "请输入备注",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontSize = 13.sp,   // 输入框字体大小
                )
            }
            // 输入框内容
            innerTextField()
            
            // 右上角字数统计
            Text(
                text = "${noteText.length}/$maxCharLimit",
                color = if (noteText.length >= maxCharLimit) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 4.dp)
            )
        }
    }
)
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 右侧“历史备注”小按钮
            // 底部居中  居底部
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    onClick = onHistoryClick,
                    color = primaryBlue,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(56.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.fillMaxSize()
                                ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary, // 按钮图标颜色
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            "历史备注",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

    }
}