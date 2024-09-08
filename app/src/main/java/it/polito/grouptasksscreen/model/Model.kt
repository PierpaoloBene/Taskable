package it.polito.grouptasksscreen.model

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
import com.google.firebase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDateTime
import java.time.ZoneOffset

class Model(context: Context) {

    /***
    +  DATABASE CONNECTION
     */

    val db = Firebase.firestore
    val dblinkPartOne = "https://taskable.page.link/?link=https://taskable.com/?teamId="
    val dblinkPartTwo = "&apn=it.polito.grouptasksscreen"



    val storage = Firebase.storage
    val myContext = context



    val imagesUrl = "gs://grouptasksscreen.appspot.com/"

    /***
     *  TEAMS REQUESTS
     */
    fun getTeams(userId: String): Flow<List<Team>> = callbackFlow {

        val query = db.collection("teams")
            .whereArrayContains("members", userId)
            .orderBy("name")

        val listener = query.addSnapshotListener { snapshot, exception ->
            if (exception != null) {
                Log.d("yyy", "eccezione: ${exception.toString()}")
                trySend(emptyList()) // Invia una lista vuota in caso di errore
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val teamsDeferred = snapshot.documents.map { document ->
                    async {
                        val timestamp = document.getTimestamp("creationTimestamp")!!
                        val imageUrl = imagesUrl + document.getString("groupImageId")
                        val team = getImageDownloadUri(imageUrl)?.let {
                            document.toObject<Team>()!!.copy(
                                id = document.id,
                                groupImageUri = it
                            )
                        }
                        team?.creationDate = LocalDateTime.ofEpochSecond(
                            timestamp.seconds,
                            timestamp.nanoseconds,
                            ZoneOffset.UTC
                        )
                        team
                    }
                }

                // Attendi il completamento di tutte le coroutine e ottieni i team
                launch {
                    val teams = teamsDeferred.awaitAll().filterNotNull()
                    trySend(teams) // Invia la lista dei team recuperati
                }
            } else {
                trySend(emptyList()) // Invia una lista vuota se lo snapshot è null
            }
        }

        awaitClose { listener.remove() } // Rimuove il listener quando il flusso si chiude
    }

    fun getTeamNameById(teamId: String): Flow<String> = callbackFlow {
        val teamdoc = db.collection("teams").document(teamId).get().await()
        val teamName = teamdoc.getString("name")

        if(teamName.isNullOrEmpty()){
            trySend("-1")
        }else{
            trySend(teamName)
        }

        awaitClose{this.cancel()}
    }

    fun getTeamProfileImageById(teamId: String): Flow<Uri> = callbackFlow {
        val teamDoc = db.collection("teams").document(teamId).get().await()
        val teamImageId = teamDoc.getString("groupImageId")

        if (teamImageId != null) {
            val imageUri = imagesUrl + teamImageId
            val downloadUri = getImageDownloadUri(imageUri)
            if (downloadUri != null) {
                trySend(downloadUri)
            } else {
                // Handle the case where the URI could not be fetched
                trySend(Uri.EMPTY)
            }
        } else {
            // Handle the case where teamImageId is null
            trySend(Uri.EMPTY)
        }

        awaitClose { this.cancel() }
    }

    fun getTeamById(teamId: String): Flow<Team> = callbackFlow {
        if (teamId.isNotEmpty() && teamId!= "-1") {
            val teamdoc = db.collection("teams")
                .document(teamId)
                .get()
                .await()

            launch {
                if (teamdoc != null) {
                    val imageUrI = imagesUrl + teamdoc.getString("groupImageId")
                    val teamDeferred = async {
                        val creationTimestamp = teamdoc.getTimestamp("creationTimestamp")!!
                        val teamDeferred = getImageDownloadUri(imageUrI)?.let {
                            teamdoc.toObject<Team>()!!.copy(

                                id = teamdoc.id,
                                creationDate = LocalDateTime.ofEpochSecond(
                                    creationTimestamp.seconds,
                                    creationTimestamp.nanoseconds,
                                    ZoneOffset.UTC
                                ),
                                groupImageUri = it
                            )

                        }
                        teamDeferred

                    }
                    val team = teamDeferred.await()
                    if (team!=null){
                        trySend(team)
                    }else{
                        trySend(Team(id = "-1"))
                    }
                } else
                    trySend(
                        Team(id = "-1")
                    )
            }
        } else {
            trySend(Team(id = "-1"))
        }

        awaitClose { this.cancel() }
    }



    fun getTeamByUserIds(userIds: List<String>): Flow<List<Team>> = callbackFlow {
        if (userIds.isNotEmpty()) {
            val query = db.collection("teams").whereArrayContainsAny("userList", userIds)

            val listenerRegistration = query.addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.d("ERROR", "Error getting teams: $error")
                    trySend(emptyList<Team>()).isFailure
                    return@addSnapshotListener
                }

                val teamsDeferred = snapshots?.documents?.map { document ->
                    async {
                        val imageUrI = imagesUrl + document.getString("groupImageId")
                        val creationTimestamp = document.getTimestamp("creationTimestamp")!!
                        val groupImageUri = getImageDownloadUri(imageUrI)
                        val team = groupImageUri?.let {
                            document.toObject<Team>()?.copy(
                                id = document.id,
                                creationDate = LocalDateTime.ofEpochSecond(
                                    creationTimestamp.seconds,
                                    creationTimestamp.nanoseconds,
                                    ZoneOffset.UTC
                                ),
                                groupImageUri = it
                            )
                        }
                        team
                    }
                } ?: emptyList()

                launch {
                    val teams = teamsDeferred.mapNotNull { it.await() }
                    trySend(teams).isSuccess
                }
            }

            awaitClose { listenerRegistration.remove() }
        } else {
            trySend(emptyList<Team>()).isFailure
        }
    }

    fun removeTeam(teamId: String) {
        if (teamId.isNotEmpty() && teamId != "-1") {
            val docRef = db.collection("teams").document(teamId)
            docRef.get().addOnSuccessListener {
                val chatId = it.getString("chatId")

                // Delete the chat document if chatId exists
                chatId?.let {
                    db.collection("chats").document(chatId).delete()
                        .addOnSuccessListener {
                            Log.d(
                                "CHAT DELETED",
                                "Chat successfully deleted with ID: $chatId"
                            )
                            //elimina team
                            docRef.delete().addOnSuccessListener {
                                Log.d("TEAM DELETED", "Team successfully deleted with ID: $teamId")
                                Toast.makeText(myContext, "Team successfully deleted!", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }.addOnFailureListener { e ->
                            Log.d("ERROR", "Error deleting chat with ID: $chatId", e)
                        }
                }

                //delete all the tasks
                db.collection("tasks").whereEqualTo("team", teamId).get()
                    .addOnSuccessListener { taskSnapshot ->
                        for (taskDoc in taskSnapshot.documents) {
                            taskDoc.reference.delete().addOnSuccessListener {
                                Log.d(
                                    "TASK DELETED",
                                    "Task ${taskDoc.id} successfully deleted"
                                )
                            }.addOnFailureListener { e ->
                                Log.d(
                                    "ERROR",
                                    "Error deleting task for team ID: $teamId",
                                    e
                                )
                            }
                        }
                    }.addOnFailureListener { e ->
                        Log.d("ERROR", "Error finding tasks for team ID: $teamId", e)
                    }
                // Remove team ID from users' teams arrays
                db.collection("users").whereArrayContains("teams", teamId).get()
                    .addOnSuccessListener { usersSnapshot ->
                        for (userDoc in usersSnapshot.documents) {
                            userDoc.reference.update(
                                "teams", FieldValue.arrayRemove(teamId)
                            ).addOnSuccessListener {
                                Log.d(
                                    "USER TEAM UPDATED",
                                    "Team ID $teamId removed from user ${userDoc.id}"
                                )
                            }.addOnFailureListener { e ->
                                Log.d(
                                    "ERROR",
                                    "Error removing team ID $teamId from user ${userDoc.id}",
                                    e
                                )
                            }
                        }
                    }.addOnFailureListener { e ->
                        Log.d("ERROR", "Error finding users with team ID: $teamId", e)
                    }
            }
        }
    }

    fun createNewTeam(teamObj: Team, callback: (String?) -> Unit) {
          var photoName = if (teamObj.groupImageUri != Uri.EMPTY) {
            LocalDateTime.now().toString() + ".png"
        }else{ "profile1.png" }
        //carica la foto nel db se presente
        if(teamObj.groupImageUri != Uri.EMPTY){
            photoName = LocalDateTime.now().toString() + ".png"
            val storageRef = storage.reference.child(photoName)
            storageRef.putFile(teamObj.groupImageUri)

        }
        // Converte la data di creazione in un oggetto Timestamp
        val creationTimestamp = Timestamp(
            teamObj.creationDate.toEpochSecond(ZoneOffset.UTC),
            teamObj.creationDate.nano
        )



        // Mappa contenente i dati del nuovo team
        val teamData = hashMapOf(
            "name" to teamObj.name,
            "description" to teamObj.description,
            "category" to teamObj.category,
            "members" to teamObj.members,
            "groupImageId" to photoName,
            "creationTimestamp" to creationTimestamp,
            "invitationLink" to "",
            "ownerId" to teamObj.ownerId
        )

        // Aggiunge il nuovo team al database
        db.collection("teams")
            .add(teamData)
            .addOnSuccessListener { documentReference ->
                val newTeamId = documentReference.id

                val invitationLink = dblinkPartOne + newTeamId + dblinkPartTwo

                db.collection("teams")
                    .document(newTeamId)
                    .update("invitationLink",invitationLink)
                    .addOnSuccessListener {
                        Log.d("SUCCESS", "New team added with ID: $newTeamId")
                        Toast.makeText(myContext, "New team created successfully!", Toast.LENGTH_SHORT).show()
                        callback(newTeamId)
                    }
                    .addOnFailureListener { e ->
                        Log.d("ERROR", "Error updating invitation link: $e")
                        Toast.makeText(myContext, "Error creating new team!", Toast.LENGTH_SHORT).show()
                        callback(null)
                    }
            }
            .addOnFailureListener { e ->
                Log.d("ERROR", "Error adding new team: $e")
                Toast.makeText(myContext, "Error creating new team!", Toast.LENGTH_SHORT).show()
                callback(null)
            }
    }

    fun deleteUserFromTeam(teamId: String, userId: String) {
        val teamRef = db.collection("teams").document(teamId)
        teamRef.update("members", FieldValue.arrayRemove(userId))
            .addOnSuccessListener {
                Log.d("TEAM UPDATED", "User ID $userId removed from team $teamId")
                val userRef = db.collection("users").document(userId)
                userRef.update("teams", FieldValue.arrayRemove(teamId))
                    .addOnSuccessListener {
                        Log.d("USER UPDATED", "Team ID $teamId removed from user $userId")
                        val tasksRef = db.collection("tasks").whereEqualTo("team", teamId)
                        tasksRef.get().addOnSuccessListener { snapshot ->
                            for (taskDoc in snapshot.documents) {
                                val taskRef = taskDoc.reference
                                taskRef.update("assignedUser", FieldValue.arrayRemove(userId))
                                    .addOnSuccessListener {
                                        Log.d("TASK UPDATED", "User ID $userId removed from task ${taskDoc.id}")
                                    }
                                    .addOnFailureListener { e ->
                                        Log.d("ERROR", "Error removing user ID $userId from task ${taskDoc.id}", e)
                                    }
                            }
                        }.addOnFailureListener { e ->
                            Log.d("ERROR", "Error querying tasks for team $teamId", e)
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.d("ERROR", "Error removing team ID $teamId from user $userId", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.d("ERROR", "Error removing user ID $userId from team $teamId", e)
            }
    }

    fun updateTaskStatus(taskId: String, newStatus: String) {
        if (taskId.isNotEmpty()) {
            val taskDocRef = db.collection("tasks").document(taskId)

            taskDocRef.update("status", newStatus)
                .addOnSuccessListener {
                    Log.d("TASK UPDATED", "Task status successfully updated for task ID: $taskId")
                    Toast.makeText(myContext, "Status updated successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Log.d("ERROR", "Error updating task status for task ID: $taskId", e)
                    Toast.makeText(myContext, "Something went wrong with the update of the status", Toast.LENGTH_SHORT).show()
                }
        } else {
            Log.d("ERROR", "Invalid task ID")
            Toast.makeText(myContext, "Invalid task ID", Toast.LENGTH_SHORT).show()
        }
    }

    fun joinTask(taskId: String, userId: String) {
        val taskRef = db.collection("tasks").document(taskId)
        taskRef.update("assignedUser", FieldValue.arrayUnion(userId)).addOnSuccessListener {
            Log.d("SUCCESS", "User $userId added to task $taskId")
            Toast.makeText(myContext, "Welcome to the task", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { e ->
            Log.d("ERROR", "Error adding user $userId to task $taskId", e)
            Toast.makeText(myContext, "Something went wrong with the update of the members", Toast.LENGTH_SHORT).show()
        }
    }

    fun updateTeamPhoto(teamId: String, uri: Uri) {
        val storage = Firebase.storage
        val db = Firebase.firestore
        val photoName = LocalDateTime.now().toString() + ".png"

        // Percorso del file nel bucket di Firebase Storage
        val storageRef = storage.reference.child(photoName)

        // Carica il file su Firebase Storage
        val uploadRef = storageRef.putFile(uri)
        uploadRef.addOnSuccessListener {
            // Aggiorna il documento dell'utente con il nome dell'immagine
            val teamRef = db.collection("teams").document(teamId)
            teamRef.update("groupImageId", photoName)
                .addOnSuccessListener {
                    Log.d("SUCCESS", "Profile photo URL updated successfully for team $teamId!")
                }
                .addOnFailureListener { e ->
                    Log.d("ERROR", "Failed to update profile photo URL for team $teamId: $e")
                }

        }.addOnFailureListener { e ->
            Log.d("ERROR", "Failed to upload profile photo for user $teamId: $e")
        }
    }

    /***
     *  TASKS REQUESTS
     */
    fun editTeam(teamId: String, teamobj: Team) {
        val db = Firebase.firestore

        val teamMap = mapOf(
            "name" to teamobj.name,
            "description" to teamobj.description,
            "category" to teamobj.category,
            "members" to teamobj.members,
            "groupImageId" to teamobj.groupImageId,
            "creationTimestamp" to Timestamp(
                teamobj.creationDate.toEpochSecond(ZoneOffset.UTC),
                teamobj.creationDate.nano
            )
        )
        val userRef= db.collection("users")

        db.collection("teams")
            .document(teamId)
            .update(teamMap)
            .addOnSuccessListener {
                //Aggiorno la lista di team per gli utenti
                db.collection("users")
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        querySnapshot.documents.forEach { document ->
                            val userRef = document.reference
                            val userTeams = document.get("teams") as? List<String> ?: emptyList()
                            if (userTeams.contains(teamId) && !teamobj.members.contains(document.id)) {
                                userRef.update("teams", FieldValue.arrayRemove(teamId))
                                    .addOnSuccessListener {
                                        Log.d(
                                            "USER UPDATED",
                                            "Team ID $teamId removed from user ${document.id}"
                                        )
                                    }
                                    .addOnFailureListener { e ->
                                        Log.d(
                                            "ERROR",
                                            "Error removing team ID $teamId from user ${document.id}",
                                            e
                                        )
                                    }
                            }
                            if(!userTeams.contains(teamId) && teamobj.members.contains(document.id)){
                                userRef.update("teams", FieldValue.arrayUnion(teamId))
                                    .addOnSuccessListener {
                                        Log.d(
                                            "USER UPDATED",
                                            "Team ID $teamId removed from user ${document.id}"
                                        )
                                    }
                                    .addOnFailureListener { e ->
                                        Log.d(
                                            "ERROR",
                                            "Error removing team ID $teamId from user ${document.id}",
                                            e
                                        )
                                    }
                            }
                        }
                    }



                // Aggiorna la lista dei membri della chat
                CoroutineScope(Dispatchers.Main).launch {
                    updateTeamChatMembers(teamobj.chatId, teamobj.members).collect { success ->
                        if (success) {
                            Toast.makeText(myContext, "Team successfully updated!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(myContext, "Team updated, but failed to patch Chat Members.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }




            }

            .addOnFailureListener { e ->
                Toast.makeText(myContext, "ERROR updating team!", Toast.LENGTH_SHORT).show()
            }
    }

    // Funzione per aggiungere una History a un task nel database Firestore
    fun addHistoryToTask(taskId: String, history: History) {

        val taskRef = db.collection("tasks").document(taskId)

        // Creazione di un nuovo oggetto History nel formato desiderato per il database
        val historyMap = hashMapOf(
            "text" to history.text,
            "creationTimestamp" to Timestamp(
                history.creationDate.toEpochSecond(ZoneOffset.UTC),
                history.creationDate.nano
            )
               )

        // Aggiunta della history all'array "history" del documento del task
        taskRef.update("history", FieldValue.arrayUnion(historyMap))
            .addOnSuccessListener {
                println("History aggiunta con successo al task con ID: $taskId")
            }
            .addOnFailureListener { e ->
                println("Errore durante l'aggiunta della history al task con ID $taskId: ${e.message}")
            }
    }
    fun getTasksByTeam(teamId: String, sorting: String? = null): Flow<List<Task>> = callbackFlow {
        val query = if (sorting != null) {
            db.collection("tasks").whereEqualTo("team", teamId).orderBy(sorting)
        } else {
            db.collection("tasks").whereEqualTo("team", teamId)
        }

        if (teamId.isNotEmpty()) {
            val listener = query.addSnapshotListener { snapshot, e ->
                if (e != null) {
                    close(e)
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val tasks = snapshot.documents.mapNotNull { document ->
                        val creationTimestamp = document.getTimestamp("creationTimestamp")!!
                        val dueTimestamp = document.getTimestamp("dueTimestamp")!!
                        val dueLocalDateTime = LocalDateTime.ofEpochSecond(
                            dueTimestamp.seconds,
                            dueTimestamp.nanoseconds,
                            ZoneOffset.UTC
                        )
                        if(dueLocalDateTime.isBefore(LocalDateTime.now())){
                            document.reference.update("status", Status.OVERDUE.toString())
                        }

                        val comments = document.get("comments") as? List<Map<String, Any>> ?: emptyList()
                        val updatedComments = comments.map { comment ->
                            val timestamp = comment["timestamp"] as Timestamp
                            val localDateTime = LocalDateTime.ofEpochSecond(
                                timestamp.seconds,
                                timestamp.nanoseconds,
                                ZoneOffset.UTC
                            )
                            Comment(
                                text = comment["text"] as? String ?: "",
                                author = comment["author"] as? String ?: "",
                                date = localDateTime
                            )
                        }

                        val history = document.get("history") as? List<Map<String, Any>> ?: emptyList()
                        val updatedHistory = history.map { historyItem ->
                            val timestamp = historyItem["creationTimestamp"] as Timestamp
                            History(
                                text = historyItem["text"] as? String ?: "",
                                creationDate = LocalDateTime.ofEpochSecond(
                                    timestamp.seconds,
                                    timestamp.nanoseconds,
                                    ZoneOffset.UTC
                                )
                            )
                        }

                        document.toObject<Task>()?.copy(
                            id = document.id,
                            comments = updatedComments,
                            creationDate = LocalDateTime.ofEpochSecond(
                                creationTimestamp.seconds,
                                creationTimestamp.nanoseconds,
                                ZoneOffset.UTC
                            ),
                            dueDate = LocalDateTime.ofEpochSecond(
                                dueTimestamp.seconds,
                                dueTimestamp.nanoseconds,
                                ZoneOffset.UTC
                            ),
                            history = updatedHistory
                        )
                    }
                    trySend(tasks).isSuccess
                } else {
                    trySend(emptyList()).isSuccess
                }
            }

            awaitClose { listener.remove() }
        } else {
            trySend(emptyList()).isSuccess
            awaitClose { }
        }
    }

    fun getTasksByUser(userId: String): Flow<List<Task>> = callbackFlow {
        val query = db.collection("tasks").whereArrayContains("assignedUser", userId)

        if (userId.isNotEmpty()) {
            val tasks = query
                .get()
                .await()
                .documents
                .mapNotNull { document ->
                    val creationTimestamp = document.getTimestamp("creationTimestamp")!!
                    val dueTimestamp = document.getTimestamp("dueTimestamp")!!
                    val comments = document.get("comments") as? List<Map<String, Any>> ?: emptyList()
                    val updatedComments = comments.map { comment ->
                        val timestamp = comment["timestamp"] as Timestamp
                        val localDateTime = LocalDateTime.ofEpochSecond(
                            timestamp.seconds,
                            timestamp.nanoseconds,
                            ZoneOffset.UTC
                        )
                        Comment(
                            text = comment["text"] as? String ?: "",
                            author = comment["author"] as? String ?: "",
                            date = localDateTime
                        )
                    }

                    val history = document.get("history") as? List<Map<String, Any>> ?: emptyList()
                    val updatedHistory = history.map { history ->
                        val timestamp = history["creationTimestamp"] as Timestamp  // Assuming date is stored as String
                            History(
                            text = history["text"] as? String ?: "",
                                creationDate = LocalDateTime.ofEpochSecond(timestamp.seconds,timestamp.nanoseconds, ZoneOffset.UTC)
                            )
                    }

                    val task = document.toObject<Task>()!!.copy(
                        id = document.id,
                        comments = updatedComments,
                        creationDate = LocalDateTime.ofEpochSecond(
                            creationTimestamp.seconds,
                            creationTimestamp.nanoseconds,
                            ZoneOffset.UTC
                        ),
                        dueDate = LocalDateTime.ofEpochSecond(
                            dueTimestamp.seconds,
                            dueTimestamp.nanoseconds,
                            ZoneOffset.UTC
                        ),
                        history = updatedHistory  // Assigning updated history to task
                    )

                    task
                }

            trySend(tasks)
        } else {
            trySend(emptyList())
        }

        awaitClose { this.cancel() }
    }

    fun getTeamIdByTaskId(taskId: String): Flow<String> = callbackFlow {
        if (taskId.isNotEmpty()) {
            val taskDoc = db.collection("tasks")
                .document(taskId)
                .get()
                .await()

            val teamId = taskDoc.getString("team")

            if (teamId != null) {
                trySend(teamId)
            } else {
                trySend("")
            }
        } else {
            trySend("")
        }

        awaitClose { this.cancel() }
    }

    fun getTaskById(taskId: String): Flow<Task> = callbackFlow {
        if (taskId.isNotEmpty() && taskId != "-1") {
            val listener = db.collection("tasks")
                .document(taskId)
                .addSnapshotListener { r, e ->
                    if (r != null) {
                        val creationTimestamp = r.getTimestamp("creationTimestamp")!!
                        val dueTimestamp = r.getTimestamp("dueTimestamp")!!
                        val comments = r.get("comments") as? List<Map<String, Any>> ?: emptyList()
                        val updatedComments = comments.map { comment ->
                            val timestamp = comment["timestamp"] as Timestamp
                            val localDateTime = LocalDateTime.ofEpochSecond(
                                timestamp.seconds,
                                timestamp.nanoseconds,
                                ZoneOffset.UTC
                            )
                            Comment(
                                text = comment["text"] as? String ?: "",
                                author = comment["author"] as? String ?: "",
                                date = localDateTime
                            )
                        }

                        val history = r.get("history") as? List<Map<String, Any>> ?: emptyList()
                        val updatedHistory = history.map { history ->
                            val timestamp = history["creationTimestamp"] as Timestamp  // Assuming date is stored as String
                            History(
                                text = history["text"] as? String ?: "",
                                creationDate = LocalDateTime.ofEpochSecond(timestamp.seconds,timestamp.nanoseconds, ZoneOffset.UTC)
                            )
                        }

                        val attachments = r.get("attachments") as? List<String> ?: emptyList()

                        val task = r.toObject(Task::class.java)?.copy(
                            id = r.id,
                            comments = updatedComments,
                            creationDate = LocalDateTime.ofEpochSecond(
                                creationTimestamp.seconds,
                                creationTimestamp.nanoseconds,
                                ZoneOffset.UTC
                            ),
                            dueDate = LocalDateTime.ofEpochSecond(
                                dueTimestamp.seconds,
                                dueTimestamp.nanoseconds,
                                ZoneOffset.UTC
                            ),
                            history = updatedHistory,
                            attachments = attachments
                        )

                        if (task != null) {
                            trySend(task)
                        } else {
                            trySend(Task(id = "-1"))
                        }
                    } else {
                        trySend(Task(id = "-1"))
                    }
                }

            awaitClose { listener.remove() }
        } else {
            trySend(Task(id = "-1"))
        }

        awaitClose { this.cancel() }
    }

    fun addTask(task: Task) {
        val newTask = hashMapOf<String, Any>()
        newTask["title"] = task.title
        newTask["description"] = task.description
        newTask["assignedUser"] = task.assignedUser
        newTask["comments"] = task.comments
        newTask["creationTimestamp"] = Timestamp(task.creationDate.toEpochSecond(ZoneOffset.UTC), task.creationDate.nano)
        newTask["dueTimestamp"] = Timestamp(task.dueDate.toEpochSecond(ZoneOffset.UTC), task.dueDate.nano)
        newTask["status"] = task.status.toString()
        newTask["tagList"] = task.tagList
        newTask["priority"] = task.priority.toString()
        newTask["team"] = task.team
        newTask["attachments"] = task.attachments

        // Check if history is not empty before adding it to newTask
        if (task.history.isNotEmpty()) {
            newTask["history"] = task.history.map {
                mapOf("text" to it.text, "creationTimestamp" to Timestamp(
                    it.creationDate.toEpochSecond(ZoneOffset.UTC),
                    it.creationDate.nano
                ))
            }
        } else {
            newTask["history"] = emptyList<History>()
        }

        if (task != Task()) {
            val docRef = db.collection("tasks").document()
            docRef.set(newTask)
                .addOnSuccessListener {
                    Toast.makeText(myContext, "Task successfully added!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(myContext, "ERROR!: ${e.message}", Toast.LENGTH_LONG).show()
                }
        } else {
            Log.d("ERROR", "No task found")
        }
    }

    fun editTask(task: Task) {
        if (task.id.isNotEmpty() && task.id != "-1") {
            val editedTask = hashMapOf<String, Any>(
                "title" to task.title,
                "description" to task.description,
                "assignedUser" to task.assignedUser,
                "comments" to task.comments.map { mapOf("text" to it.text, "date" to it.date.toString()) },
                "creationTimestamp" to Timestamp(task.creationDate.toEpochSecond(ZoneOffset.UTC), task.creationDate.nano),
                "dueTimestamp" to Timestamp(task.dueDate.toEpochSecond(ZoneOffset.UTC), task.dueDate.nano),
                "status" to task.status.toString(),
                "tagList" to task.tagList,
                "priority" to task.priority.toString(),
                "id" to task.id
            )



            val docRef = db.collection("tasks").document(task.id)
            docRef.set(editedTask, SetOptions.merge())
                .addOnSuccessListener {
                    addHistoryToTask(task.id,History("Task Updated", LocalDateTime.now()))
                    Toast.makeText(myContext, "Task successfully updated!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(myContext, "ERROR!: ${e.message}", Toast.LENGTH_LONG).show()
                }
        } else {
            Log.d("ERROR", "Task ID is missing or invalid")
        }
    }

    fun removeTask(id: String){
        if (id.isNotEmpty() && id != "-1"){
            val docRef = db.collection("tasks").document(id)

            docRef.delete()
                .addOnSuccessListener {
                    Log.d("TASK DELETED", "Task successfully deleted with ID: $id")
                    Toast.makeText(myContext, "Task successfully removed!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Log.d("ERROR", "Error deleting task with ID: $id")
                }

        }
    }


   //ATTACHAMENTS FUNCTIONS
    fun uploadAttachment(taskId: String,fileName: String, uri: Uri, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val storageRef = storage.reference.child("tasks/$taskId/$fileName.pdf")

        // Carica il file su Firebase Storage
        val uploadRef = storageRef.putFile(uri)
        uploadRef.addOnSuccessListener {
            // Aggiorna il documento dell'utente con il nome dell'immagine
            val taskRef = db.collection("tasks").document(taskId)
            taskRef.update("attachments", FieldValue.arrayUnion("$fileName.pdf"))
                .addOnSuccessListener {
                    Log.d("SUCCESS", "Task attachments updated successfully for task $taskId!")
                }
                .addOnFailureListener { e ->
                    Log.d("ERROR", "Failed to update Task attachments: $e")
                }

        }.addOnFailureListener { e ->
            Log.d("ERROR", "Failed to upload profile photo for user $taskId: $e")
        }
    }


    fun getAttachment(taskId: String, fileName: String, onSuccess: (Uri) -> Unit) {
        val docRef = db.collection("tasks").document(taskId)

        // Recupera il documento che contiene l'URL dell'allegato
        docRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                        // Scarica il file dallo storage Firebase
                        storage.getReference("tasks/$taskId/$fileName").downloadUrl
                            .addOnSuccessListener { uri ->

                                onSuccess.invoke(uri)
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(myContext, "Error downloading attachment: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(myContext,"Attachment URL is empty", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(myContext,"No Such File", Toast.LENGTH_SHORT).show()
            }
    }

    // Funzione per rimuovere un allegato dal database e dal documento del task
    fun removeAttachment(taskId: String, fileName: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {


        // Riferimento al documento del task
        val docRef = db.collection("tasks").document(taskId)

        // Elimina il file dallo storage Firebase
        val storageRef = storage.reference.child("tasks/$taskId/$fileName")
        storageRef.delete()
            .addOnSuccessListener {
                // Rimuovi il riferimento dell'allegato dal documento del task in Firestore
                docRef.get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            val attachments = document.get("attachments") as? List<String>

                            if (attachments != null && attachments.contains(fileName)) {
                                val updatedAttachments = attachments.toMutableList()
                                updatedAttachments.remove(fileName)

                                // Aggiorna il documento del task con gli allegati aggiornati
                                docRef.update("attachments", updatedAttachments)
                                    .addOnSuccessListener {
                                        onSuccess.invoke()
                                    }
                                    .addOnFailureListener { e ->
                                        onFailure.invoke("Error updating task document: ${e.message}")
                                    }
                            } else {
                                onFailure.invoke("Attachment not found in task document")
                            }
                        } else {
                            onFailure.invoke("Task document not found")
                        }
                    }
                    .addOnFailureListener { e ->
                        onFailure.invoke("Error fetching task document: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                onFailure.invoke("Error deleting attachment: ${e.message}")
            }
    }



    /***
     *  MEMBERS API
     */
    fun getMembers(userIds: List<String>): Flow<List<User>> = callbackFlow {

        Log.d("xxx", "prima: $userIds")

        if (userIds.isNotEmpty()) {
            val membersDeferred = db.collection("users")
                .get()
                .await()
                .documents
                .filter { it.id in userIds }
                .mapNotNull {document->
                    val imageUri = imagesUrl + document.getString("profilePhotoId")
                    async {
                        getImageDownloadUri(imageUri)?.let { uri ->
                            document.toObject<User>()!!.copy(
                                id = document.id,
                                profilePhotoUri = uri
                            )
                        }
                    }
                }
            launch {
                val members = membersDeferred.awaitAll().filterNotNull()
                Log.d("xxx", "inviati: ${members.map { it.id }}")
                trySend(members)
            }


        } else {
            Log.d("xxx", "e' andata male")
            trySend(emptyList())
        }

        awaitClose { this.cancel() }
    }

    fun getAllMembers(): Flow<List<User>> = callbackFlow {
        val listener = db.collection("users").orderBy("nickname").addSnapshotListener { r, e ->
            if (r != null) {
                val membersDeferred = r.documents.mapNotNull { document ->
                    val imageUri = imagesUrl + document.getString("profilePhotoId")
                    async {
                        getImageDownloadUri(imageUri)?.let { uri ->
                            document.toObject<User>()!!.copy(
                                id = document.id,
                                profilePhotoUri = uri
                            )
                        }
                    }
                }

                launch {
                    val members = membersDeferred.awaitAll().filterNotNull()
                    trySend(members)
                }
            } else {
                trySend(emptyList())
            }
        }

        awaitClose { listener.remove() }
    }

    fun getMemberByMail(email: String): Flow<String> = callbackFlow {

        val query = db.collection("users").whereEqualTo("email",email)

        if (email.isNotEmpty()) {
            val userId = query
                .get()
                .await()
                .documents.firstNotNullOf { document ->
                    val userId = document.id
                    userId
                }
            trySend(userId)
        }else{
            trySend("-1")
        }

    awaitClose{ this.cancel()}

    }
    fun addMemberToTeam(teamId: String, userId: String) {
        val db = Firebase.firestore


        // Aggiorna la lista di ID degli utenti nel campo "members" del documento del team
        db.collection("teams")
            .document(teamId)
            .update("members", FieldValue.arrayUnion(userId))
            .addOnSuccessListener {
                // Aggiorna anche l'utente nel database
                db.runTransaction { transaction ->
                    val userRef = db.collection("users").document(userId)
                    transaction.update(userRef, "teams", FieldValue.arrayUnion(teamId))
                }.addOnSuccessListener {
                    // Successo
                }.addOnFailureListener { e ->
                    Toast.makeText(
                        myContext,
                        "Errore durante l'aggiunta del team all'utente: $e",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    myContext,
                    "Errore durante l'aggiunta dell'utente al team: $e",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }


    fun addMembersToTeam(teamId: String, members: List<User>) {
        val db = Firebase.firestore

        // Crea una lista di ID degli utenti
        val userIds = members.map { it.id }

        // Aggiorna la lista di ID degli utenti nel campo "users" del documento del team
        db.collection("teams")
            .document(teamId)
            .update("members", FieldValue.arrayUnion(*userIds.toTypedArray()))
            .addOnSuccessListener {
                Toast.makeText(
                    myContext,
                    "Utenti aggiunti con successo al team!",
                    Toast.LENGTH_SHORT
                ).show()

                // Aggiorna anche l'utente nel database
                db.runTransaction { transaction ->
                    for (userId in userIds) {
                        val userRef = db.collection("users").document(userId)
                        transaction.update(userRef, "teams", FieldValue.arrayUnion(teamId))
                    }
                }.addOnSuccessListener {

                }.addOnFailureListener { e ->
                    Toast.makeText(
                        myContext,
                        "Errore durante l'aggiunta del team agli utenti: $e",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    myContext,
                    "Errore durante l'aggiunta degli utenti al team: $e",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    fun getTaskMembers(taskId: String): Flow<List<User>> = callbackFlow {
        if (taskId.isNotEmpty() && taskId != "-1") {
            val userIds: List<String> = db.collection("tasks")
                .document(taskId)
                .get()
                .await()
                .get("assignedUser").let { it as? List<String> ?: emptyList() }
            getMembers(userIds).collect {
                trySend(it)
            }
        }else{
            trySend(emptyList())
        }
        awaitClose { this.cancel() }
    }

    fun getTeamMembersExcludingList(teamId: String, excludeUsers: List<User>): Flow<List<User>> = callbackFlow {
        if (teamId.isNotEmpty()) {
            val excludeUserIds = excludeUsers.map { it.id }.toSet()

            val listener = db.collection("users")
                .whereArrayContains("teams", teamId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("ERROR", "Listen failed.", error)
                        close(error)
                        return@addSnapshotListener
                    }

                    if (snapshot != null && !snapshot.isEmpty) {
                        val members = snapshot.documents
                            .mapNotNull { it.toObject<User>()?.copy(id = it.id) }
                            .filter { !excludeUserIds.contains(it.id) }

                        trySend(members)
                    } else {
                        Log.d("ERROR", "No users found")
                        trySend(emptyList())
                    }
                }

            awaitClose { listener.remove() }
        } else {
            Log.d("ERROR", "No team found with this id $teamId")
            trySend(emptyList())
            awaitClose {}
        }
    }

    fun getTeamMembersNotInTask(taskId: String): Flow<List<User>> = callbackFlow {
        if (taskId.isNotEmpty()) {
            try {
                // Step 1: Retrieve the team ID associated with the task ID
                val taskDoc = db.collection("tasks")
                    .document(taskId)
                    .get()
                    .await()

                val teamId = taskDoc.getString("team") ?: ""

                if (teamId.isNotEmpty()) {
                    // Step 2: Retrieve the members of the team
                    val teamDoc = db.collection("teams")
                        .document(teamId)
                        .get()
                        .await()

                    val teamMembersIds = teamDoc.get("members") as? List<String> ?: emptyList()

                    // Step 3: Retrieve the members assigned to the task
                    val taskMembersIds = taskDoc.get("members") as? List<String> ?: emptyList()

                    // Step 4: Filter team members to exclude those in the task
                    val filteredMembersIds = teamMembersIds.filter { it !in taskMembersIds }

                    // Retrieve the user objects for the filtered member IDs
                    val members = db.collection("users")
                        .whereIn("id", filteredMembersIds)
                        .get()
                        .await()
                        .documents
                        .mapNotNull { it.toObject(User::class.java)?.copy(id = it.id) }
                    trySend(members)
                } else {
                    Log.d("ERROR", "No team found for this task")
                    trySend(emptyList())
                }
            } catch (e: Exception) {
                Log.d("ERROR", "Failed to retrieve members: ${e.message}")
                trySend(emptyList())
            }
        } else {
            Log.d("ERROR", "No task ID provided")
            trySend(emptyList())
        }

        awaitClose { this.cancel() }
    }

    fun getMembersByTeam(teamId: String): Flow<List<User>> = callbackFlow {
        val listener = db.collection("users")
            .whereArrayContains("teams", teamId)
            .addSnapshotListener { r, e ->
                if (e != null) {
                    close(e)
                    return@addSnapshotListener
                }

                val members = r?.documents?.mapNotNull { doc ->
                    val imageUrl = imagesUrl + doc.getString("profilePhotoId")
                    val userDeferred = async {
                        try {
                            val image = getImageDownloadUri(imageUrl)
                            val user =
                                if (image == null){
                                    doc.toObject(User::class.java)?.copy(
                                        id = doc.id,
                                        profilePhotoUri = Uri.EMPTY
                                    )
                                }else{
                                    doc.toObject(User::class.java)?.copy(
                                        id = doc.id,
                                        profilePhotoUri = image
                                    )
                                }

                            /*val user =
                                    getImageDownloadUri(imageUrl)?.let {
                                        doc.toObject(User::class.java)?.copy(
                                            id = doc.id,
                                            profilePhotoUri = it
                                        )
                                    }*/
                            user
                        } catch (e: Exception) {
                            Log.e("ERROR", "Failed to get user: $e")
                            null
                        }
                    }
                    userDeferred
                } ?: emptyList()

                launch {
                    val users = members.awaitAll().filterNotNull()
                    trySend(users)
                }
            }

        awaitClose { listener.remove() }
    }

    fun getMembersByDiffTeam(teamId: String): Flow<List<User>> = callbackFlow {
        if (teamId.isNotEmpty()) {
            val membersDeferred = db.collection("users")
                .get()
                .await()
                .documents
                .mapNotNull {
                    val imageUri = imagesUrl + it.getString("profilePhotoId")
                    async{
                        getImageDownloadUri(imageUri)?.let { it1 ->
                            it.toObject<User>()?.copy(
                                id = it.id,
                                profilePhotoUri = it1
                            )
                        }
                    }

                }

            launch {
                val members = membersDeferred.awaitAll().filterNotNull().filter{!it.teams.contains(teamId)}
                trySend(members)
            }
        } else {
            Log.d("ERROR", "No team found with this id $teamId")
            trySend(emptyList())
        }

        awaitClose { this.cancel() }

    }

    fun getMembersByDiffTeam(teamId: String, filterUsers: List<User>): Flow<List<User>> =
        callbackFlow {
            if (teamId.isNotEmpty()) {
                val filterUserIds =
                    filterUsers.map { it.id }.toSet()  // Ottieni gli ID degli utenti da filtrare

                val listenerRegistration = db.collection("users")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.e("ERROR", "Listen failed.", error)
                            close(error)
                            return@addSnapshotListener
                        }

                        if (snapshot != null && !snapshot.isEmpty) {
                            val membersDeferred = snapshot.documents
                                .mapNotNull {
                                    val imageUri = imagesUrl + it.getString("profilePhotoId")
                                    async{
                                        getImageDownloadUri(imageUri)?.let { it1 ->
                                            it.toObject<User>()?.copy(
                                                id = it.id,
                                                profilePhotoUri = it1
                                            )
                                        }
                                    }
                                }

                            launch {
                                val members = membersDeferred.awaitAll().filterNotNull().filter{!it.teams.contains(teamId) && !filterUserIds.contains(it.id)}
                                trySend(members)
                            }

                        } else {
                            Log.d("ERROR", "No users found")
                            trySend(emptyList())
                        }
                    }

                awaitClose { listenerRegistration.remove() }
            } else {
                Log.d("ERROR", "No team found with this id $teamId")
                trySend(emptyList())
                awaitClose {}
            }
        }

    fun getMemberById(userId: String): Flow<User> = callbackFlow {
        if (userId.isNotEmpty()) {
            val memberDoc = db.collection("users")
                .document(userId)
                .get()
                .await()
            val imageUrI = imagesUrl + memberDoc.getString("profilePhotoId")
            val memberDeferred = async {
                val memberDeferredAsync=getImageDownloadUri(imageUrI)?.let {
                    memberDoc.toObject<User>()?.copy(
                        id = memberDoc.id,
                        profilePhotoUri = it
                    )
                }
                memberDeferredAsync
            }
            launch {
                if (memberDoc != null) {
                    val member = memberDeferred.await()
                    trySend(member?:User(nickname = "-1"))
                } else
                    trySend(
                        User(nickname = "-1")
                    )
            }

        } else {
            Log.d("ERROR", "No members found for this task")
            trySend(User(nickname = "-1"))
        }

        awaitClose { this.close() }
    }

    fun getTeamMembersId(teamId: String): Flow<List<String>> = callbackFlow {
        val docRef = db.collection("teams").document(teamId)

        val listener = docRef.addSnapshotListener { snapshot, _ ->
            if (snapshot != null && snapshot.exists()) {
                val members = snapshot.get("members").let { it as? List<String> ?: emptyList() }
                trySend(members).isSuccess

            } else {
                trySend(emptyList<String>()).isSuccess
            }
        }

        awaitClose { listener.remove() }
    }


    fun getTeamName(teamId: String): Flow<String> = callbackFlow {
        val teamName = db.collection("teams").document(teamId).get().await().getString("name")
        trySend(teamName.toString())

        awaitClose { this.cancel() }
    }
    /***
     *  CHAT API
     */
    // return the chatID
    // it has to be called when a new team is created
    fun createTeamChat(teamID: String): Flow<String> = callbackFlow {
        val docRef = db.collection("chats").document()
        val chat = HashMap<String, Any>()
        chat["team"] = teamID
        chat["messagesList"] = emptyList<Comment>()
        val members = db.collection("teams").document(teamID).get().await().get("members") as List<String>

        chat["userList"] = members

        docRef.set(chat)
            .addOnSuccessListener {
                trySend(docRef.id)
            }
            .addOnFailureListener { e ->
                Log.d("ERROR", "Error creating chat: $e")
                trySend("")
            }
        awaitClose { this.cancel() }
    }

    fun updateTeamChatMembers(chatId: String, members: List<String>): Flow<Boolean> = callbackFlow {
        val chatDocRef = db.collection("chats").document(chatId)

        // Update the userList in the chat document
        chatDocRef.update("userList", members)
            .addOnSuccessListener {
                trySend(true)
            }
            .addOnFailureListener { e ->
                Log.d("ERROR", "Error updating chat members: $e")
                trySend(false)
            }

        awaitClose { this.cancel() }
    }


    fun createPersonalChat(users: List<String>): Flow<String> = callbackFlow {
        if (users.isNotEmpty() && users.size == 2) {
            val chatId = db.collection("chats").document().id
            val chat = hashMapOf(
                "messagesList" to emptyList<Comment>(),
                "userList" to users,
                "team" to null
            )
            db.collection("chats").document(chatId).set(chat)
                .addOnSuccessListener {
                    trySend(chatId).isSuccess
                }
                .addOnFailureListener { e ->
                    Log.d("ERROR", "Error creating chat: $e")
                    trySend("").isFailure
                }
        } else {
            Log.d("ERROR", "Invalid users list")
            trySend("").isFailure
        }
        awaitClose { this.cancel() }
    }

    fun getPrivateChatId(users: List<String>): Flow<String> = callbackFlow {
        if (users.isNotEmpty() && users.size == 2) {
            val listenerRegistration = db.collection("chats")
                .whereArrayContains("userList", users[0])
                .whereEqualTo("team", null)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.d("ERROR", "Error getting chat: $error")
                        trySend("").isFailure
                        return@addSnapshotListener
                    }

                    val chatId = snapshot?.documents?.firstOrNull{ document ->
                        val userList = document.get("userList") as? List<*>
                        userList?.contains(users[1]) == true
                    }?.id

                    if (chatId != null) {
                        trySend(chatId).isSuccess
                    } else {
                        launch {
                            createPersonalChat(users).collect { newChatId ->
                                trySend(newChatId).isSuccess
                            }
                        }
                    }
                }

            awaitClose { listenerRegistration.remove() }
        } else {
            Log.d("ERROR", "Invalid users list")
            trySend("").isFailure
        }
    }

    fun getChatById(chatId: String): Flow<List<Comment>> = callbackFlow {
        if (chatId.isNotEmpty()) {
            val listenerRegistration = db.collection("chats")
                .document(chatId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.d("ERROR", "Error getting chat: $error")
                        trySend(emptyList()).isFailure
                        return@addSnapshotListener
                    }

                    val chatList: List<Comment> = snapshot?.get("messagesList")
                        ?.let { it as? List<Map<String, Any>> ?: emptyList() }
                        ?.map {
                            val timestamp = it.get("timestamp") as Timestamp
                            val localDateTime = LocalDateTime.ofEpochSecond(
                                timestamp.seconds,
                                timestamp.nanoseconds,
                                ZoneOffset.UTC
                            )
                            Comment(
                                text = it.get("text") as? String ?: "",
                                author = it.get("author") as? String ?: "",
                                date = localDateTime
                            )
                        } ?: emptyList()
                    trySend(chatList).isSuccess
                }

            awaitClose { listenerRegistration.remove() }
        } else {
            Log.d("ERROR", "No chat found with this id $chatId")
            trySend(emptyList()).isFailure
        }
    }

    fun getTeamIdbyChatId(chatId: String): Flow<String> = callbackFlow {
        val docRef = db.collection("chats").document(chatId)
        val subscription = docRef.addSnapshotListener { snapshot, _ ->
            if (snapshot != null && snapshot.exists()) {
                val teamId = snapshot.getString("team") as? String ?: ""
                trySend(teamId).isSuccess
            }else{
                trySend("")
            }
        }
        awaitClose{ subscription.remove()}
    }

    fun getChatIdByTeamId(teamId: String): Flow<String> = callbackFlow{
        val docRef = db.collection("teams").document(teamId)

        val subscription = docRef.addSnapshotListener{
                snapshot, _ ->
            if(snapshot != null && snapshot.exists()) {
                val chatId = snapshot.getString("chatId") as? String ?: ""
                trySend(chatId).isSuccess
            }
        }

        awaitClose { subscription.remove() }
    }

    fun getChatIdsByUserId(userId: String): Flow<List<String>> = callbackFlow {
        if (userId.isNotEmpty()) {
            val query = db.collection("chats").whereArrayContains("userList", userId)

            val listenerRegistration = query.addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.d("ERROR", "Error getting chats: $error")
                    trySend(emptyList()).isFailure
                    return@addSnapshotListener
                }

                val chatIds = snapshots?.documents?.map { it.id } ?: emptyList()

                trySend(chatIds).isSuccess
            }

            awaitClose { listenerRegistration.remove() }
        } else {
            Log.d("ERROR", "No user found with this id $userId")
            trySend(emptyList()).isFailure
        }
    }

    fun addMessageToChat(chatId: String, newMessage: Comment) {
        val messageMap = mapOf(
            "author" to newMessage.author,
            "text" to newMessage.text,
            "timestamp" to Timestamp(
                newMessage.date.toEpochSecond(ZoneOffset.UTC),
                newMessage.date.nano
            )
        )
        if(chatId.isNotEmpty() && chatId.isNotBlank()){
            val docRef = db.collection("chats")
                .document(chatId)
            docRef.update("messagesList", FieldValue.arrayUnion(messageMap))
                .addOnSuccessListener {
                    Log.d("MESSAGE ADDED", "Message successfully added to chat with ID: $chatId")
                }
                .addOnFailureListener { e ->
                    Log.w("ERROR", "Error adding message to chat: $e")
                }
        }
    }


    fun getChatByChatId(chatId: String): Flow<Chat> = callbackFlow {
        val chatDoc = db.collection("chats").document(chatId).get().await()

        if (chatDoc.exists()) {
            val teamId = chatDoc.getString("team") ?: "-1"

            // Convert messagesList to a list of Comment objects
            val messagesListData = chatDoc.get("messagesList") as List<Map<String, Any>>? ?: emptyList()
            val messagesList = messagesListData.map { data ->
                val timestamp = data["timestamp"] as com.google.firebase.Timestamp
                val localDateTime = LocalDateTime.ofEpochSecond(
                    timestamp.seconds,
                    timestamp.nanoseconds,
                    ZoneOffset.UTC
                )
                Comment(
                    text = data["text"] as String,
                    author = data["author"] as String,
                    date = localDateTime
                )
            }

            val userIds = chatDoc.get("userList") as List<String>? ?: emptyList()

            // Retrieve user details with profilePhotoUri
            val membersDeferred = userIds.map { userId ->
                async {
                    val userDoc = db.collection("users").document(userId).get().await()
                    userDoc.toObject(User::class.java)?.let { user ->

                        val profilePhotoId = userDoc.getString("profilePhotoId")
                        val profilePhotoUri = imagesUrl + profilePhotoId
                        getImageDownloadUri(profilePhotoUri)?.let { uri ->
                            user.copy(
                                id = userId,
                                profilePhotoUri = uri)
                        }
                    }
                }
            }

            launch {
                val userList = membersDeferred.awaitAll().filterNotNull()
                val chat = Chat(
                    chatId = chatId,
                    teamId = teamId,
                    messagesList = messagesList,
                    userList = userList
                )
                trySend(chat)
            }
        } else {
            trySend(Chat(chatId = "-1"))
        }

        awaitClose { this.cancel() }
    }



    /***
     *  COMMENTS API
     */
    fun addCommentToTask(taskId: String, newComment: Comment) {
        //val db = Firebase.firestore

        val commentMap = mapOf(
            "text" to newComment.text,
            "author" to newComment.author,
            "timestamp" to Timestamp(
                newComment.date.toEpochSecond(ZoneOffset.UTC),
                newComment.date.nano
            )
        )

        db.collection("tasks")
            .document(taskId)
            .update("comments", FieldValue.arrayUnion(commentMap))
            .addOnSuccessListener {
                Toast
                    .makeText(myContext, "Comment succesfully added!", Toast.LENGTH_SHORT)
                    .show()
            }
            .addOnFailureListener { e ->
                Toast
                    .makeText(myContext, "ERROR!", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    /***
     *  USERS API
     */
    fun deleteUsers(teamId: String, users: List<User>) {
        val db = Firebase.firestore

        // Per ogni utente nella lista
        users.forEach { user ->
            // Verifica se l'utente ha il teamId nel campo "teams"
            if (user.teams.contains(teamId)) {
                // Rimuovi il teamId dall'array "teams" nel documento dell'utente
                db.collection("users")
                    .document(user.id)
                    .update("teams", FieldValue.arrayRemove(teamId))
                    .addOnSuccessListener {
                        Log.d("SUCCESS", "User ${user.id} removed from team successfully!")

                        // Ora puoi aggiornare la lista dei membri del team, rimuovendo l'utente
                        db.collection("teams")
                            .document(teamId)
                            .update("members", FieldValue.arrayRemove(user.id))
                            .addOnSuccessListener {
                                Log.d(
                                    "SUCCESS",
                                    "User ${user.id} removed from team members successfully!"
                                )
                            }
                            .addOnFailureListener { e ->
                                Log.d(
                                    "ERROR",
                                    "Failed to remove user ${user.id} from team members: $e"
                                )
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.d("ERROR", "Failed to remove user ${user.id} from team: $e")
                    }
            } else {
                Log.d("INFO", "User ${user.id} is not a member of team $teamId, no action taken.")
            }
        }
    }

    fun addUsers(teamId: String, users: List<User>) {
        val db = Firebase.firestore

        // Per ogni utente nella lista
        users.forEach { user ->
            // Aggiungi il teamId all'array "teams" nel documento dell'utente
            db.collection("users")
                .document(user.id)
                .update("teams", FieldValue.arrayUnion(teamId))
                .addOnSuccessListener {
                    Log.d("SUCCESS", "Team $teamId added to user ${user.id} successfully!")

                    // Ora puoi aggiornare la lista dei membri del team, aggiungendo l'utente
                    db.collection("teams")
                        .document(teamId)
                        .update("members", FieldValue.arrayUnion(user.id))
                        .addOnSuccessListener {
                            Log.d("SUCCESS", "User ${user.id} added to team members successfully!")
                        }
                        .addOnFailureListener { e ->
                            Log.d("ERROR", "Failed to add user ${user.id} to team members: $e")
                        }
                }
                .addOnFailureListener { e ->
                    Log.d("ERROR", "Failed to add team $teamId to user ${user.id}: $e")
                }
        }
    }

    fun updateUser(userId: String, updatedUser: User) {
        val db = Firebase.firestore
        val userRef = db.collection("users").document(userId)

        // Crea una mappa con i campi da aggiornare
        val updates = hashMapOf<String, Any>(
            "fullName" to updatedUser.fullName,
            "nickname" to updatedUser.nickname,
            "email" to updatedUser.email,
            "telephone" to updatedUser.telephone,
            "location" to updatedUser.location,
            "description" to updatedUser.description,
            "teams" to updatedUser.teams,
            //  "tasks" to updatedUser.tasks,
            "nOfTaskCompleted" to updatedUser.nOfTaskCompleted,
            //  "ratings" to updatedUser.ratings,
            "mean" to updatedUser.mean,
            "role" to updatedUser.role,
            "skills" to updatedUser.skills,
            "achievements" to updatedUser.achievements
        )

        // Esegui l'aggiornamento
        userRef.update(updates)
            .addOnSuccessListener {
                Log.d("SUCCESS", "User $userId updated successfully!")
            }
            .addOnFailureListener { e ->
                Log.d("ERROR", "Failed to update user $userId: $e")
            }
    }

    fun uploadProfilePhoto(userId: String, uri: Uri) {
        val photoName = LocalDateTime.now().toString() + ".png"

        // Percorso del file nel bucket di Firebase Storage
        val storageRef = storage.reference.child(photoName)

        // Carica il file su Firebase Storage
        val uploadRef = storageRef.putFile(uri)
        uploadRef.addOnSuccessListener {
            // Aggiorna il documento dell'utente con il nome dell'immagine
            val userRef = db.collection("users").document(userId)
            userRef.update("profilePhotoId", photoName)
                .addOnSuccessListener {
                    Log.d("SUCCESS", "Profile photo URL updated successfully for user $userId!")
                }
                .addOnFailureListener { e ->
                    Log.d("ERROR", "Failed to update profile photo URL for user $userId: $e")
                }

        }.addOnFailureListener { e ->
            Log.d("ERROR", "Failed to upload profile photo for user $userId: $e")
        }
    }

    fun updateTeamWithChatId(teamId: String, chatId: String, callback: (Boolean) -> Unit) {
        val teamRef = db.collection("teams").document(teamId)
        teamRef.update("chatId", chatId)
            .addOnSuccessListener {
                Log.d("SUCCESS", "Team updated with chatId: $chatId")
                callback(true)
            }
            .addOnFailureListener { e ->
                Log.d("ERROR", "Error updating team with chatId: $e")
                callback(false)
            }
    }

    fun getAllTag(): Flow<List<String>> = callbackFlow {
        val listener = db.collection("tasks").addSnapshotListener { snapshot, exception ->
            if (exception != null) {
                Log.d("ERROR", exception.toString())
                trySend(emptyList())
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val tags = snapshot.documents
                    .mapNotNull { it.get("tagList") as? List<String> }
                    .flatten()
                    .distinct()
                trySend(tags)
            } else {
                trySend(emptyList())
            }
        }

        awaitClose { listener.remove() }
    }


    private suspend fun getImageDownloadUri(imageUrl: String): Uri? {
        return try {
            val storageRef = storage.getReferenceFromUrl(imageUrl)
            val downloadUrl = storageRef.downloadUrl.await()
            downloadUrl
        } catch (e: Exception) {
            Log.e("ERROR", "Failed to get download URL: $e")
            null
        }
    }

    //notifications

    fun getUserNotification(userId: String): Flow<List<Notification>> = callbackFlow {
        val query = db.collection("notifications").whereEqualTo("userId", userId)

        val listener = query.addSnapshotListener{ snapshot, error ->
            val notifications = snapshot?.documents?.flatMap { document ->
                val notificationList = document["notificationList"] as? List<Map<String, Any>> ?: emptyList()
                notificationList.mapNotNull { notificationMap ->
                    val timestamp = notificationMap["creationTimestamp"] as? Timestamp
                    val text = notificationMap["text"] as? String
                    val readFlag = notificationMap["readFlag"] as? Boolean

                    if (timestamp != null && text != null && readFlag != null) {
                        Notification(
                            id = document.id,
                            text = text,
                            readFlag = readFlag,
                            creationDate = LocalDateTime.ofEpochSecond(
                                timestamp.seconds,
                                timestamp.nanoseconds,
                                ZoneOffset.UTC
                            )
                        )
                    } else {
                        null
                    }
                }
            } ?: emptyList()
            trySend(notifications)
        }

        awaitClose{ listener.remove()}
    }

    fun updateNotificationReadFlag(notificationId: String, index: Int) {
        if (notificationId.isNotEmpty()) {
            val notificationDocRef = db.collection("notifications").document(notificationId)

            notificationDocRef.get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val notificationsList = document["notificationList"] as? List<Map<String, Any>>
                        if (notificationsList != null && index >= 0 && index < notificationsList.size) {
                            val notificationMap = notificationsList[index]
                            val updatedNotificationMap = notificationMap.toMutableMap()
                            updatedNotificationMap["readFlag"] = true

                            val updatedNotificationsList = notificationsList.toMutableList()
                            updatedNotificationsList[index.toInt()] = updatedNotificationMap

                            notificationDocRef.update("notificationList", updatedNotificationsList)
                                .addOnSuccessListener {
                                    Log.d("NOTIFICATION UPDATED", "Notification readFlag successfully updated for notification ID: $notificationId")
                                    Toast.makeText(myContext, "Notification marked as read", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener { e ->
                                    Log.d("ERROR", "Error updating readFlag for notification ID: $notificationId", e)
                                    Toast.makeText(myContext, "Something went wrong while updating the notification", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            Log.d("ERROR", "Invalid index")
                            Toast.makeText(myContext, "Invalid index", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.d("ERROR", "Document does not exist")
                        Toast.makeText(myContext, "Document does not exist", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Log.d("ERROR", "Error fetching document for notification ID: $notificationId", e)
                    Toast.makeText(myContext, "Something went wrong while fetching the document", Toast.LENGTH_SHORT).show()
                }
        } else {
            Log.d("ERROR", "Invalid notification ID")
            Toast.makeText(myContext, "Invalid notification ID", Toast.LENGTH_SHORT).show()
        }
    }

    fun addNotificationToUsers(userIds: List<String>, notification: Notification) {

        val notificationMap = mapOf(
            "text" to notification.text,
            "readFlag" to notification.readFlag,
            "creationTimestamp" to Timestamp(
                notification.creationDate.toEpochSecond(ZoneOffset.UTC),
                notification.creationDate.nano
            )
        )

        for (userId in userIds) {
            val userDocRef = db.collection("notifications").whereEqualTo("userId", userId)

            userDocRef.get().addOnSuccessListener { documents ->
                for (document in documents) {
                    val documentRef = document.reference
                    documentRef.update("notificationList", FieldValue.arrayUnion(notificationMap))
                        .addOnSuccessListener {
                            Log.d("NOTIFICATION ADDED", "Notification successfully added to user ID: $userId")
                        }
                        .addOnFailureListener { e ->
                            Log.d("ERROR", "Error adding notification to user ID: $userId", e)
                        }
                }
            }.addOnFailureListener { e ->
                Log.d("ERROR", "Error fetching document for user ID: $userId", e)
            }
        }
    }

    fun clearReadNotifications(userId: String) {

        val query = db.collection("notifications").whereEqualTo("userId", userId).get()
            .addOnSuccessListener { notificationSnapshot ->
                for (doc in notificationSnapshot.documents) {
                    val notificationList = doc["notificationList"] as? List<Map<String, Any>> ?: emptyList()

                    val updatedNotificationList = notificationList.filterNot { notification ->
                        notification["readFlag"] == true
                    }

                    doc.reference
                        .update("notificationList", updatedNotificationList)
                        .addOnSuccessListener {
                            Toast.makeText(myContext, "Read Notification Cleared", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(myContext, "Error clearing notifications", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error fetching notifications", e)
            }

    }

    fun updateUserRatings(usersMap: Map<String, Int>, taskId: String, loggedUserId: String) {
        val usersCollection = db.collection("users")
        val userDoc = db.collection("users").document(loggedUserId)

        usersMap.forEach { (userId, rating) ->
            val userDocRef = usersCollection.document(userId)
            val userIds :MutableList<String> = emptyList<String>().toMutableList()
            userIds.add(userId)

            db.runTransaction { transaction ->
                val userSnapshot = transaction.get(userDocRef)
                val currentTotalReviews = userSnapshot.getLong("totalReviews") ?: 0
                val currentMean = userSnapshot.getDouble("mean") ?: 0.0

                val newTotalReviews = currentTotalReviews + 1
                val newMean = ((currentMean * currentTotalReviews) + rating) / newTotalReviews

                transaction.update(userDocRef, "totalReviews", newTotalReviews)
                transaction.update(userDocRef, "mean", newMean)
                addNotificationToUsers(
                    userIds,
                    Notification(
                        text = "You have just received a $rating ★ review!",
                        creationDate = LocalDateTime.now()
                    )
                )
            }.addOnSuccessListener {
                userDoc.update("reviews", FieldValue.arrayUnion(taskId))
                    .addOnSuccessListener {
                        Log.d("UpdateUserRatings", "Successfully updated reviewedBy field for taskId: $taskId")
                    }
                    .addOnFailureListener { e ->
                        Log.e("UpdateUserRatings", "Error updating reviewedBy field for taskId: $taskId", e)
                    }
            }.addOnFailureListener { e ->
                Toast.makeText(myContext, "Something went wrong submiting the ratings", Toast.LENGTH_SHORT).show()
                Log.d("ERROR", "Error submitting the ratings", e)
            }
        }
    }

    fun getUserReviews(userId: String): Flow<List<String>> = callbackFlow {
        val userDocRef = db.collection("users").document(userId)

        val listener = userDocRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Toast.makeText(myContext, "Something went wrong getting the ratings", Toast.LENGTH_SHORT).show()
                Log.d("ERROR", "Error getting the ratings", error)
            }

            if (snapshot != null && snapshot.exists()) {
                val reviews = snapshot.get("reviews") as? List<String> ?: emptyList()
                trySend(reviews)
            } else {
                trySend(emptyList())
            }
        }

        awaitClose { listener.remove() }
    }

    fun removeReviewFromUsers(userIds: List<String>, taskId: String) {
        val usersCollection = db.collection("users")

        for (userId in userIds) {
            val userDocRef = usersCollection.document(userId)

            userDocRef.get().addOnSuccessListener { document ->
                if (document.exists()) {
                    if (document.contains("reviews")) {
                        userDocRef.update("reviews", FieldValue.arrayRemove(taskId))
                            .addOnSuccessListener {
                                println("Successfully removed task $taskId from user $userId's reviews")
                            }
                            .addOnFailureListener { e ->
                                println("Error removing task $taskId from user $userId's reviews: $e")
                            }
                    } else {
                        println("User $userId does not have a 'reviews' field")
                    }
                } else {
                    println("User document with ID $userId does not exist")
                }
            }.addOnFailureListener { e ->
                println("Error fetching user document with ID $userId: $e")
            }
        }
    }

    fun UnReatedMembersToast(){
        Toast.makeText(myContext, "Rank all members to submit", Toast.LENGTH_SHORT).show()
    }

}
