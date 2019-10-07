package com.eccos.socialyou

import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText

import com.firebase.client.Firebase
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.text.SimpleDateFormat

import android.preference.PreferenceManager
import android.view.inputmethod.InputMethodManager
import android.widget.ProgressBar
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.add_event_form.*
import java.text.ParseException
import java.util.*
import kotlin.collections.HashMap


class AddEventForm : AppCompatActivity() {
    private var fusedLocationClient: FusedLocationProviderClient? = null

    private var locationCallback: LocationCallback? = null

    private var myFirebaseRef: Firebase? = null

    private var locationRequestNew: LocationRequest? = null

    private var storageRef: StorageReference? = null

    private var mImageUri: Uri? = null

    private var spotsSwiped = ArrayList<String>()

    private var title: EditText? = null
    private var date: EditText? = null
    private var time: EditText? = null
    private var location: EditText? = null
    private var description: EditText? = null


    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Firebase.setAndroidContext(this)
        setContentView(R.layout.add_event_form)


        //Getting references to Firebase
        storageRef = FirebaseStorage.getInstance().reference

        title = findViewById(R.id.title)
        date = findViewById(R.id.date)
        time = findViewById(R.id.time)
        location = findViewById(R.id.location)
        description = findViewById(R.id.description)

        setDialogCalendar()

        findViewById<ConstraintLayout>(R.id.constraintLayout).setOnTouchListener(View.OnTouchListener { v, event ->
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(slogan.windowToken, 0)
            constraintLayout.requestFocus()

            true
        })

        val chooseImage = findViewById<View>(R.id.choose_image) as Button
        chooseImage.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), MY_WRITE_PERMISSION_REQUEST)
            }
        }


        val sendButton = findViewById<View>(R.id.send_button) as Button
        sendButton.setOnClickListener {
            if (checkTextFields()) {

                if (mImageUri != null) {
                    val sendButton = findViewById<Button>(R.id.send_button)
                    sendButton.visibility = View.INVISIBLE

                    val progressBar = findViewById<ProgressBar>(R.id.progressBar)
                    progressBar.visibility = View.VISIBLE

                    saveToFirebase()
                } else {
                    showSnackbar(R.string.select_image)
                }
            }
        }

        showSnackbar(R.string.add_event_message)

    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            MY_WRITE_PERMISSION_REQUEST ->

                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //permission was granted do nothing and carry on
                    openFileChooser()
                } else {
                    showSnackbar(R.string.grant_storage)
                }
        }
    }

    private fun openFileChooser() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK
                && data != null && data.data != null) {
            mImageUri = data.data

            showSnackbar(R.string.image_selected)
        }
    }

    private fun checkTextFields(): Boolean {
        if (TextUtils.isEmpty(title!!.text)) {
            /***   You can Toast a message here that the title is Empty  */
            title!!.error = "Title is required!"

        } else if (TextUtils.isEmpty(date!!.text)) {
            date!!.error = "Date is required!"

        } else if (TextUtils.isEmpty(time!!.text)) {
            time!!.error = "Time is required!"

        } else if (TextUtils.isEmpty(location!!.text)) {
            location!!.error = "Location is required!"

        } else if (TextUtils.isEmpty(description!!.text)) {
            description!!.error = "Description is required!"

        } else {
            return true
        }

        return false
    }


    private fun saveToFirebase() {
        try {
            fusedLocationClient!!.lastLocation
                    .addOnSuccessListener(this@AddEventForm) { myLocation ->
                        // Got last known location. In some rare situations this can be null.

                        if (myLocation != null) {
                            //final LatLng myLocation = new LatLng(location.getLatitude(), location.getLongitude());

                            Log.e("Cord", "Latitude: " + myLocation.latitude + " Longitude: " + myLocation.longitude)

                            //Getting reference to Firebase
                            myFirebaseRef = Firebase("https://socialyou-be6cf.firebaseio.com/")

                            val mInformation = HashMap<String, Any>()


                            val userId = FirebaseAuth.getInstance().currentUser!!.uid


                            //Saving all data with FireBase
                            mInformation.put("title", title!!.text.toString())
                            mInformation.put("date", date!!.text.toString())
                            mInformation.put("time", time!!.text.toString())
                            mInformation.put("location", location!!.text.toString())
                            mInformation.put("description", description!!.text.toString())
                            val fb = myFirebaseRef!!.child("events").push()
                            fb.setValue(mInformation)

                            val key = fb.key

                            //Creating a location node with GeoFire
                            val ref = FirebaseDatabase.getInstance().getReference("/locations")
                            val geoFire = GeoFire(ref)

                            geoFire.setLocation(key, GeoLocation(myLocation.latitude, myLocation.longitude)) { key, error ->
                                if (error != null) {
                                    Log.e(TAG, "There was an error saving the location to GeoFire: $error")
                                } else {
                                    Log.e(TAG, "Location saved on server successfully!")
                                }
                            }

                            //Creating a node for event's attendees
                            myFirebaseRef!!.child("attendees").child(key).child(userId).setValue(true)

                            //Linking the event to the user
                            myFirebaseRef!!.child("users").child(userId).child("events").child(key).setValue(true)

                            //Uploading the event image with FireBase Storage
                            storeImageFile(key)

                            addSpotSwiped(key)

                        } else {
                            Log.e(TAG, "Location is null.")

                            val contextView = this@AddEventForm.findViewById<View>(android.R.id.content)

                            Snackbar.make(contextView, R.string.location_null, Snackbar.LENGTH_LONG)
                                    .show()
                        }
                    }

        } catch (ex: Exception) {
            Log.e(TAG, "Exception: $ex")
        }

    }

    private fun storeImageFile(path: String) {
        //        Uri file = mImageUri;
        val ref = storageRef!!.child(path)

        try {
            val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, mImageUri)

            val resized = Bitmap.createScaledBitmap(bitmap, 600, 800, true)

            val resizedUri = getImageUri(this, resized)

            ref.putFile(resizedUri)
                    .addOnSuccessListener { taskSnapshot ->
                        // Get a URL to the uploaded content
                        val downloadUrl = taskSnapshot.uploadSessionUri
                        Log.e(TAG, "" + downloadUrl!!)

                        val handler = Handler()
                        handler.postDelayed({
                            val myIntent = Intent(this@AddEventForm, MyEvents::class.java)
                            startActivity(myIntent)

                            finish()

                        }, 0)
                    }
                    .addOnFailureListener {
                        // Handle unsuccessful uploads
                        // ...
                    }

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun getImageUri(inContext: Context, inImage: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(inContext.contentResolver, inImage,
                "SocialYou - What and Who is Next to Me", "Find more about the app on the PlayStore.")
        return Uri.parse(path)
    }

    private fun addSpotSwiped(key: String) {
        var pref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val json = pref.getString("spotsSwiped", null)

        val collectionType = object : TypeToken<ArrayList<String>>() {}.type
        spotsSwiped = Gson().fromJson(json, collectionType)

        spotsSwiped.add(key)

        val json1 = Gson().toJson(spotsSwiped)
        pref.edit().putString("spotsSwiped", json1).commit()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(slogan.windowToken, 0)
    }

    private fun setDialogCalendar() {
        val myCalendar = Calendar.getInstance()


        date!!.setOnClickListener {
            DatePickerDialog(this@AddEventForm, DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
                myCalendar.set(Calendar.YEAR, year)
                myCalendar.set(Calendar.MONTH, monthOfYear)
                myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                updateLabel(myCalendar)
            }, myCalendar.get(Calendar.YEAR), myCalendar.get(Calendar.MONTH), myCalendar.get(Calendar.DAY_OF_MONTH)).show()
        }


        date!!.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            if (date!!.hasFocus()) {
                DatePickerDialog(this@AddEventForm, DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
                    myCalendar.set(Calendar.YEAR, year)
                    myCalendar.set(Calendar.MONTH, monthOfYear)
                    myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    updateLabel(myCalendar)
                }, myCalendar.get(Calendar.YEAR), myCalendar.get(Calendar.MONTH), myCalendar.get(Calendar.DAY_OF_MONTH)).show()
            }
        }

        time!!.setOnClickListener {
            TimePickerDialog(this@AddEventForm,
                    TimePickerDialog.OnTimeSetListener { view, hourOfDay, minute ->
                        val dateTime = getFormatedDateTime("$hourOfDay:$minute", "HH:mm", "hh:mm a")
                        time!!.setText(dateTime)
                    }, myCalendar.get(Calendar.HOUR_OF_DAY), myCalendar.get(Calendar.MINUTE), false).show()
        }


        time!!.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            if (time!!.hasFocus()) {
                TimePickerDialog(this@AddEventForm,
                        TimePickerDialog.OnTimeSetListener { view, hourOfDay, minute ->
                            val dateTime = getFormatedDateTime("$hourOfDay:$minute", "HH:mm", "hh:mm a")
                            time!!.setText(dateTime)
                        }, myCalendar.get(Calendar.HOUR_OF_DAY), myCalendar.get(Calendar.MINUTE), false).show()
            }
        }
    }


    private fun updateLabel(myCalendar: Calendar) {
        val myFormat = "MM/dd/yyyy" //In which you need put here
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault())


        date!!.setText(sdf.format(myCalendar.time))
    }

    fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this@AddEventForm, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this@AddEventForm)

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult?) {
                    if (locationResult == null) {
                        return
                    }
                }
            }

            locationRequestNew = LocationRequest.create()

            locationRequestNew!!.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            locationRequestNew!!.fastestInterval = 2500
            locationRequestNew!!.interval = 10000


            fusedLocationClient!!.requestLocationUpdates(locationRequestNew,
                    locationCallback!!, null/* Looper */)
        }
    }

    override fun onBackPressed() {
        persistBeforeActivities()
        super.onBackPressed()
    }


    private fun persistBeforeActivities() {
        var intent= Intent()
        setResult(Activity.RESULT_OK, intent)
    }

    /**
     * Called after the start and in between pauses and running.
     */
    public override fun onResume() {
        super.onResume()
        startLocationUpdates()
    }

    /**
     * Executed when the process is running on the background.
     */
    public override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    private fun stopLocationUpdates() {
        if (fusedLocationClient != null) {
            fusedLocationClient!!.removeLocationUpdates(locationCallback!!)
        }
    }


    private fun showSnackbar(text: Int) {
        val contextView = findViewById<View>(android.R.id.content)

        Snackbar.make(contextView, text, Snackbar.LENGTH_LONG)
                .show()
    }


    companion object {

        private val MY_WRITE_PERMISSION_REQUEST = 101

        private val PICK_IMAGE_REQUEST = 1

        private val TAG = "AddEventForm"
    }

    private fun getFormatedDateTime(dateStr : String, strReadFormat : String, strWriteFormat : String) : String {

        var formattedDate = dateStr

        var readFormat =  SimpleDateFormat(strReadFormat, Locale.getDefault())
        var writeFormat = SimpleDateFormat(strWriteFormat, Locale.getDefault())

        var date : Date? = null

        try {
            date = readFormat.parse(dateStr);
        } catch (e : ParseException) {
        }

        if (date != null) {
            formattedDate = writeFormat.format(date);
        }

        return formattedDate;
    }
}