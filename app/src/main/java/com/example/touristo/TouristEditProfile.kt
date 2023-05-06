package com.example.touristo

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.touristo.Fragments.TouristManagement
import com.example.touristo.adapter.AdminHomeTMAdapter
import com.example.touristo.dbCon.TouristoDB
import com.example.touristo.dialogAlerts.ConfirmationDialog
import com.example.touristo.dialogAlerts.ProgressLoader
import com.example.touristo.formData.UserProfileValidation
import com.example.touristo.formData.UserRegisterForm
import com.example.touristo.modal.User
import com.example.touristo.modal.Villa
import com.example.touristo.repository.UserRepository
import com.example.touristo.validations.ValidationResult
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class TouristEditProfile : AppCompatActivity() {
    private var count = 0;
    private lateinit var confirmationDialog : ConfirmationDialog
    private lateinit var byteArray:ByteArray
    private val PICK_IMAGE_REQUEST = 1
    private lateinit var user: User
    private lateinit var db: TouristoDB

    private var selectedImage: Bitmap? = null
    private lateinit var imageUri: Uri

    private lateinit var ImgTmEditProfileBack:ImageView

    private lateinit var smimgTmEditProfilePic:ImageView
    private lateinit var tvTmEditProfileStatus:TextView
    private lateinit var tvTmEditProfileName:TextView
    private lateinit var btnTmEditProfileStatus:TextView

    private lateinit var etTMUserEmail:EditText
    private lateinit var etTMUserPassword:EditText
    private lateinit var etTMUserTEL:EditText
    private lateinit var etTMUserCountry:EditText
    private lateinit var etTMUserAge:EditText
    private lateinit var spTMUserACT:Spinner
    private lateinit var spTMUSerGender:Spinner
    private lateinit var etTMUserName:EditText

    private lateinit var btnTMDlt:Button
    private lateinit var btnTMSave:Button

    private lateinit var aemail:String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tourist_edit_profile)

        //the if block is executed so that the notification pannel color changes and the Icon of them changes
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = ContextCompat.getColor(this, R.color.bgBackground)

            val flags = window.decorView.systemUiVisibility
            window.decorView.systemUiVisibility = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        //code starts here
        //initialize the views

        ImgTmEditProfileBack = findViewById(R.id.ImgTmEditProfileBack)
        smimgTmEditProfilePic = findViewById(R.id.smimgTmEditProfilePic)
        tvTmEditProfileStatus = findViewById(R.id.tvTmEditProfileStatus)
        tvTmEditProfileName = findViewById(R.id.tvTmEditProfileName)
        btnTmEditProfileStatus = findViewById(R.id.btnTmEditProfileStatus)

        etTMUserEmail = findViewById(R.id.etTMUserEmail)
        etTMUserPassword = findViewById(R.id.etTMUserPassword)
        etTMUserTEL = findViewById(R.id.etTMUserTEL)
        etTMUserCountry = findViewById(R.id.etTMUserCountry)
        etTMUserAge = findViewById(R.id.etTMUserAge)
        etTMUserName = findViewById(R.id.etTMUserName)
        spTMUserACT = findViewById(R.id.spTMUserACT)
        spTMUSerGender = findViewById(R.id.spTMUSerGender)
        btnTMDlt = findViewById(R.id.btnTMDlt)
        btnTMSave = findViewById(R.id.btnTMSave)
        etTMUserEmail.isEnabled=false

        smimgTmEditProfilePic.setOnClickListener {
            ImagePickerLaunch()
        }
        btnTMDlt.setOnClickListener {
            finish()
        }
        val bundle = intent.extras
        user = (bundle?.getSerializable("user") as? User)!!
        aemail = bundle.getString("amail").toString()
        byteArray = intent.extras?.getByteArray("image")!!

        if (user != null) {
            //set ET values Accordingly
            val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size ?: 0)

            smimgTmEditProfilePic.setImageBitmap(bitmap)
            tvTmEditProfileName.text = user.uname
            if(user.deleted==0){
                tvTmEditProfileStatus.setTextColor(ContextCompat.getColor(this, R.color.admincount))
                btnTmEditProfileStatus.text ="Active"
                btnTmEditProfileStatus.backgroundTintList =  ContextCompat.getColorStateList(this, R.color.admincount)
            }else if(user.deleted==1){
                tvTmEditProfileStatus.setTextColor(ContextCompat.getColor(this, R.color.bgDialogError))
                btnTmEditProfileStatus.text ="Deactive"
                btnTmEditProfileStatus.backgroundTintList =  ContextCompat.getColorStateList(this, R.color.bgDialogError)
            }

            etTMUserEmail.setText(user.uemail)
            etTMUserPassword.setText(user.password)
            etTMUserTEL.setText(user.tel)
            etTMUserCountry.setText(user.country)
            if (user.age==null){
                etTMUserAge.setText(0)
            }else{
                etTMUserAge.setText(user.age.toString())
            }
            etTMUserName.setText(user.uname)


            user.deleted?.let { spTMUserACT.setSelection(it) }
            var position:Int =0
            if(user.gender.toString() == "Male"){
                position =0
            }else if(user.gender.toString()=="Female"){
                position=1
            }
            spTMUSerGender.setSelection(position)
        }


        lifecycleScope.launch(Dispatchers.IO){
            setEditProfileForm(user)

        }

    }

     private suspend fun setEditProfileForm(user: User?) {

        ImgTmEditProfileBack.setOnClickListener {
            finish()
        }

        btnTMSave.setOnClickListener {
            //specific names for the variable

            val dbName = etTMUserName.text.toString()
            val dbEmail = etTMUserEmail.text.toString()
            val dbPassword =  etTMUserPassword.text.toString()
            val dbTel = etTMUserTEL.text.toString()
            val dbPropic = ""

            val dbAge:Int = if(etTMUserAge.text.toString().isEmpty() || etTMUserAge.text.toString()==""){
                0
            }else{
                etTMUserAge.text.toString().toInt()
            }
            val dbGender = spTMUSerGender.selectedItem.toString()
            val dbCountry = etTMUserCountry.text.toString()
            val dbActStatus = spTMUserACT.selectedItem.toString()

            var dbActStausInt : Int =0
            if(dbActStatus == "Active"){
                dbActStausInt =0
            }else if(dbActStatus == "Deactive"){
                dbActStausInt = 1
            }

            val userEditForm = UserProfileValidation(
                dbName,
                dbPassword,
                dbTel,
                dbPropic,
                dbAge.toString(),
                dbCountry
            )
            val validationAge =userEditForm.validationAge()
            val nameValidation =userEditForm.validateUserName()
            val passwordValidation =userEditForm.validatePassword()
            val telValidation =userEditForm.validateTel()
            val validateCountry =userEditForm.validateCountry()

            when(validationAge){
                is ValidationResult.Valid ->{ count ++ }
                is ValidationResult.Invalid ->{
                    lifecycleScope.launch(Dispatchers.Main) {
                        etTMUserAge.error =validationAge.errorMessage
                    }

                }
                is ValidationResult.Empty ->{
                    lifecycleScope.launch(Dispatchers.Main) {
                        etTMUserAge.error =validationAge.errorMessage
                    }

                }
            }

            when(nameValidation){
                is ValidationResult.Valid ->{ count ++ }
                is ValidationResult.Invalid ->{
                    lifecycleScope.launch(Dispatchers.Main) {
                        etTMUserName.error =nameValidation.errorMessage
                    }

                }
                is ValidationResult.Empty ->{
                    lifecycleScope.launch(Dispatchers.Main) {
                        etTMUserName.error =nameValidation.errorMessage
                    }

                }
            }

            when(passwordValidation){
                is ValidationResult.Valid ->{ count ++ }
                is ValidationResult.Invalid ->{
                    lifecycleScope.launch(Dispatchers.Main) {
                        etTMUserPassword.error =passwordValidation.errorMessage
                    }

                }
                is ValidationResult.Empty ->{
                    lifecycleScope.launch(Dispatchers.Main) {
                        etTMUserPassword.error =passwordValidation.errorMessage
                    }

                }
            }

            when(telValidation){
                is ValidationResult.Valid ->{ count ++ }
                is ValidationResult.Invalid ->{
                    lifecycleScope.launch(Dispatchers.Main) {
                        etTMUserTEL.error =telValidation.errorMessage
                    }

                }
                is ValidationResult.Empty ->{
                    lifecycleScope.launch(Dispatchers.Main) {
                        etTMUserTEL.error =telValidation.errorMessage
                    }

                }
            }
            when(validateCountry){
                is ValidationResult.Valid ->{ count ++ }
                is ValidationResult.Invalid ->{
                    lifecycleScope.launch(Dispatchers.Main) {
                        etTMUserCountry.error =validateCountry.errorMessage
                    }

                }
                is ValidationResult.Empty ->{
                    lifecycleScope.launch(Dispatchers.Main) {
                        etTMUserCountry.error =validateCountry.errorMessage
                    }

                }
            }
            if(count==5){

                lifecycleScope.launch(Dispatchers.IO){
                    db = TouristoDB.getInstance(application)
                    // Get the UserDao from the database
                    val userDao = db.userDao()
                    val userRepo = UserRepository(userDao, Dispatchers.IO)
                    val result : Int = userRepo.updateUserProfileAsAdmin(dbCountry,dbGender,dbAge,dbTel,dbPassword,dbName,dbEmail,dbActStausInt)
                    lifecycleScope.launch(Dispatchers.Main){
                        confirmationDialog = ConfirmationDialog(this@TouristEditProfile)
                        if(result>0){
                            confirmationDialog.dialogWithSuccess("Profile Updated") {
                                val intent = Intent(this@TouristEditProfile, AdminHome::class.java).apply {
                                    putExtra("replaceFragment", "TouristManagement")
                                    putExtra("adminEmail", aemail)
                                }
                                startActivity(intent)
                                finish()

                            }
                        }else{
                            confirmationDialog.dialogWithError("Invalid Credentials") {
                                //do anything
                            }
                        }
                    }
                }

                count=0
            }
            count=0
        }

    }

    private fun ImagePickerLaunch(){
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }
    override fun onActivityResult(requestCode:  Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode== RESULT_OK && PICK_IMAGE_REQUEST==1){
            imageUri = data?.data!!
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
            selectedImage = bitmap
            smimgTmEditProfilePic.setImageURI(imageUri)

            // This will give you the image name
            val uri = Uri.parse(imageUri.toString())
            val imageFile = File(uri.path!!)
            val imageName = imageFile.name



            uploadImageToFireBase(imageName,imageUri)
            updateImage(user.uemail,imageName)
        }
    }

    private fun updateImage(email: String,uri: String){
        db = TouristoDB.getInstance(application)
        // Get the UserDao from the database
        val userDao = db.userDao()
        val userRepo = UserRepository(userDao, Dispatchers.IO)

        userRepo.updateImage(uri,email)
    }
    private fun uploadImageToFireBase(fileName:String, uri: Uri){
        val progressBuilder = ProgressLoader(this@TouristEditProfile,"Fetching Image","Please Wait......")
        progressBuilder.startProgressLoader()
        val storageReference = FirebaseStorage.getInstance().getReference("UserImages/$fileName")
        storageReference.putFile(uri).addOnSuccessListener {
            progressBuilder.dismissProgressLoader()
        }.addOnFailureListener{
            progressBuilder.dismissProgressLoader()
            Toast.makeText(this@TouristEditProfile,"Error Upload", Toast.LENGTH_SHORT).show()
        }
    }
}