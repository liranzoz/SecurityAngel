package com.example.securityangel

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import com.example.securityangel.databinding.ActivityAddMemberBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AddMemberActivity : androidx.appcompat.app.AppCompatActivity() {

    private lateinit var binding: ActivityAddMemberBinding
    private var currentFamilyId: String? = null
    private var currentUser: User? = null // שומרים את המשתמש המלא כדי שנוכל להשתמש בשם שלו ליצירת המשפחה

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddMemberBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupInitialData()
        setupButtons()
    }

    private fun setupInitialData() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // טעינת המשתמש הנוכחי
        FirebaseFirestore.getInstance().collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                currentUser = document.toObject(User::class.java)
                currentFamilyId = currentUser?.familyId

                // שינוי לוגיקה: אם אין משפחה - אנחנו לא סוגרים את המסך!
                // אנחנו פשוט נדע שבזמן הלחיצה צריך קודם ליצור אחת.
                if (currentFamilyId == null) {
                    // אופציונלי: עדכון כותרת כדי שהמשתמש יבין שהוא יוצר קבוצה
                    binding.btnAddBtn.text = "Create Family & Add Member"
                }
            }
    }

    private fun setupButtons() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnAddBtn.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()

            // ולידציה
            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.tilEmail.error = "Please enter a valid email address"
                return@setOnClickListener
            }
            if (currentUser == null) return@setOnClickListener // הגנה למקרה שהנתונים עוד לא נטענו

            setLoading(true)

            // --- הצומת המרכזי: האם אנחנו מוסיפים למשפחה קיימת או יוצרים חדשה? ---

            if (currentFamilyId != null) {
                // תרחיש א': יש משפחה - מוסיפים רגיל
                addMemberToExistingFamily(currentFamilyId!!, email)
            } else {
                // תרחיש ב': אין משפחה - יוצרים חדשה ואז מוסיפים
                createNewFamilyAndAddMember(email)
            }
        }
    }

    // פונקציה ליצירת משפחה חדשה (תרחיש ב')
    private fun createNewFamilyAndAddMember(emailToAdd: String) {
        val admin = currentUser!!
        val familyName = "${admin.lastName} Family" // שם ברירת מחדל, למשל "Cohen Family"

        FamilyRepository.createFamily(
            admin = admin,
            familyName = familyName,
            onSuccess = { newFamilyId ->
                // הצלחנו ליצור משפחה! עכשיו נוסיף את החבר
                currentFamilyId = newFamilyId // מעדכנים את המשתנה המקומי
                addMemberToExistingFamily(newFamilyId, emailToAdd)
            },
            onFailure = { error ->
                setLoading(false)
                Toast.makeText(this, "Failed to create family: $error", Toast.LENGTH_LONG).show()
            }
        )
    }

    // פונקציה להוספת חבר (משותפת לשני התרחישים)
    private fun addMemberToExistingFamily(familyId: String, email: String) {
        FamilyRepository.addMemberByEmail(
            familyId = familyId,
            email = email,
            onSuccess = {
                setLoading(false)
                Toast.makeText(this, "Member added successfully!", Toast.LENGTH_SHORT).show()
                finish()
            },
            onFailure = { errorMessage ->
                setLoading(false)
                binding.tilEmail.error = errorMessage
            }
        )
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnAddBtn.isEnabled = !isLoading
        binding.etEmail.isEnabled = !isLoading
    }
}