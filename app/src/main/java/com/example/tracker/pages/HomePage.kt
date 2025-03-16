package com.example.tracker.pages


import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tracker.R
import com.example.tracker.model.HabitModel
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore


@Composable
fun HomePage(modifier: Modifier = Modifier) {

    val context = LocalContext.current
    val db = Firebase.firestore
    val userId = Firebase.auth.currentUser?.uid
    val user = Firebase.auth.currentUser

    var habits by remember { mutableStateOf<List<HabitModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    if (user == null) {
        Toast.makeText(context, "User not logged in!", Toast.LENGTH_SHORT).show()
        return
    }

    // Fetch habits from Firebase
    LaunchedEffect(userId) {
        if (userId != null) {
//            db.collection("habits")
//                .document(userId)
//                .collection("userHabits")
//                .get()
//                .addOnCompleteListener(){
//                    habits = it.result.documents.mapNotNull {  }
//                }


            db.collection("habits").document(userId).collection("userHabits")
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("Firebase", "Error fetching habits", e)
                        Toast.makeText(context, "Failed to load habits", Toast.LENGTH_SHORT).show()
                        isLoading = false
                        return@addSnapshotListener
                    }

                    // ✅ Manually extracting data instead of using `.toObject()`
                    habits = snapshot?.documents?.mapNotNull { document ->
                        val habitID = document.getString("habitID") ?: return@mapNotNull null
                        val habitName = document.getString("habitName") ?: return@mapNotNull null
                        val habitDescription = document.getString("habitDescription") ?: return@mapNotNull null
                        val streak = document.getLong("streak")?.toInt() ?: 0
                        val lastTimeCompleted = document.getTimestamp("lastTimeCompleted") ?: Timestamp.now()
                        val activeDays = document.get("activeDays") as? List<Boolean> ?: List(7) { false }

                        HabitModel(
                            habitID = habitID,
                            habitName = habitName,
                            habitDescription = habitDescription,
                            streak = streak,
                            lastTimeCompleted = lastTimeCompleted,
                            activeDays = activeDays
                        )
                    } ?: emptyList()

                    isLoading = false
                }
        }

    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            items(habits) { habit ->
                HabitItem(habit, db, userId)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun HabitItem(habit: HabitModel, db: FirebaseFirestore, userId: String?) {
    val context = LocalContext.current

    // Check if habit is completed today
    val todayIndex = (Timestamp.now().toDate().day + 6) % 7
    Toast.makeText(context, "Today Index: ${todayIndex.toString()}", Toast.LENGTH_LONG).show()

    val today = Timestamp.now().toDate()
    val lastCompletedDate = habit.lastTimeCompleted?.toDate()
    var isCompletedToday = lastCompletedDate!!.date == today.date
    val isForToday = habit.activeDays[todayIndex]

    // Colors
    val activeDayColor = Color(0xFF4CAF50) // Bright Green
    //val inactiveDayColor = Color(0xFFBDBDBD) // Subtle Greenish Gray
    val inactiveDayColor = Color.LightGray // Subtle Greenish Gray

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(Color.White, shape = RoundedCornerShape(12.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                verticalAlignment = Alignment.Top
            ) {
                // 🔥 Streak Counter with Flame Icon
                Column(
                    modifier = Modifier.padding(end = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = habit.streak.toString(),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    Icon(
                        painter = painterResource(id = if (isCompletedToday) R.drawable.flame_on else R.drawable.flame_off),
                        contentDescription = "Streak Flame",
                        modifier = Modifier.size(32.dp),
                        tint = Color.Unspecified // Keep original icon colors
                    )
                }

                Column(modifier = Modifier
                    //.padding(end = 10.dp)
                    .weight(1f)) {
                    Row() {
                        // 🏷 Habit Title (Bold)
                        Text(modifier = Modifier.weight(1f),
                            text = habit.habitName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                        )
//                        Button(
                        Button(
                            onClick = {},
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                            modifier = Modifier.padding(start = 10.dp)
                                //.align(Alignment.End)
                            //.size(50.dp)
                            //.background(Color.Yellow,
                            //    shape = RoundedCornerShape(50),
                            //),
                            //contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Edit",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // 📄 Habit Description (Small Text)
                    if (habit.habitDescription.isNotBlank()) {
                        Text(
                            text = habit.habitDescription,
                            fontSize = 14.sp,
                            color = Color.DarkGray
                        )
                    }

                    //Spacer(modifier = Modifier.height(8.dp))

                }

                // ✅ Mark Habit as Completed Button
//                Button(
//                    onClick = {
//                        if (!isCompletedToday && isForToday) {
//                            val updatedStreak =
//                                if (habit.activeDays[todayIndex]) habit.streak + 1 else 0
//                            val updatedHabit = habit.copy(
//                                streak = updatedStreak,
//                                lastTimeCompleted = Timestamp.now()
//                            )
//
//                            userId?.let {
//                                db.collection("habits")
//                                    .document(it)
//                                    .collection("userHabits")
//                                    .document(habit.habitID)
//                                    .set(updatedHabit)
//                                    .addOnSuccessListener {
//                                        Toast.makeText(
//                                            context,
//                                            "Marked as Completed!",
//                                            Toast.LENGTH_SHORT
//                                        ).show()
//                                        isCompletedToday = !isCompletedToday
//                                    }
//                                    .addOnFailureListener {
//                                        Toast.makeText(
//                                            context,
//                                            "Failed to update habit!",
//                                            Toast.LENGTH_SHORT
//                                        ).show()
//                                    }
//                            }
//                        }
//                    },
//                    colors = ButtonDefaults.buttonColors(
//                        containerColor = if (!isForToday) Color.LightGray else if (isCompletedToday) Color.Green else Color.Gray,
//                        //disabledContainerColor = if (!isForToday) Color.LightGray else if (isCompletedToday) Color.Green else Color.Gray
//                    ),
//                    //modifier = Modifier.width(60.dp),
//                    //enabled = isForToday && !isCompletedToday,
//
//                ) {
//                    Text(
//                        text = if (!isForToday) "✘" else if (isCompletedToday) "✔" else "Done?",
//                        fontSize = 13.sp,
//                        fontWeight = FontWeight.Black
//                    )
//                }
            }
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
            ) {
                // 📅 Days of the Week
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                    for (i in days.indices) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    if (habit.activeDays[i]) activeDayColor else inactiveDayColor,
                                    shape = RoundedCornerShape(30),
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = days[i],
                                fontWeight = if (i == todayIndex) FontWeight.ExtraBold else FontWeight.Normal,
                                color = if (i == todayIndex) Color(0xFFD81B60) else Color.White,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }
//                Button(
//                    onClick = {},
//                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
//                    modifier = Modifier
//                        .align(Alignment.End)
//                        //.size(50.dp)
//                        //.background(Color.Yellow,
//                        //    shape = RoundedCornerShape(50),
//                        //),
//                    //contentAlignment = Alignment.Center
//                    ){
//                    Text(
//                        text = "Edit",
//                        color = Color.White,
//                        fontSize = 14.sp
//                    )
//                }
                Button(
                    onClick = {
                        if (!isCompletedToday && isForToday) {
                            val updatedStreak =
                                if (habit.activeDays[todayIndex]) habit.streak + 1 else 0
                            val updatedHabit = habit.copy(
                                streak = updatedStreak,
                                lastTimeCompleted = Timestamp.now()
                            )

                            userId?.let {
                                db.collection("habits")
                                    .document(it)
                                    .collection("userHabits")
                                    .document(habit.habitID)
                                    .set(updatedHabit)
                                    .addOnSuccessListener {
                                        Toast.makeText(
                                            context,
                                            "Marked as Completed!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        isCompletedToday = !isCompletedToday
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(
                                            context,
                                            "Failed to update habit!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isForToday) Color.LightGray else if (isCompletedToday) Color.Green else Color.Gray,
                        //disabledContainerColor = if (!isForToday) Color.LightGray else if (isCompletedToday) Color.Green else Color.Gray
                    ),
                    modifier = Modifier.align(Alignment.End),
                    //enabled = isForToday && !isCompletedToday,

                ) {
                    Text(
                        text = if (!isForToday) "✘" else if (isCompletedToday) "✔" else "Mark Done?",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black
                    )
                }

            }
        }
    }
}
