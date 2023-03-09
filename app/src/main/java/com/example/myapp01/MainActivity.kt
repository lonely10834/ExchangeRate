package com.example.myapp01

import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import com.example.myapp01.databinding.ActivityMainBinding
import android.util.Log
import android.widget.Button
import android.widget.EditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import android.content.Context
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import java.util.Date
import java.text.SimpleDateFormat
import java.util.*
import java.io.FileWriter
import java.io.File
import android.net.ConnectivityManager
import java.io.FileNotFoundException



class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    // Define a list of currencies to support
    private val currencies = arrayOf("TWD", "USD", "JPY", "CAD")
    private var buildTime: String? = null
    // Declare a lateinit variable for InputMethodManager
    private lateinit var imm: InputMethodManager
    fun Date.toSimpleString(pattern: String): String {
        val formatter = SimpleDateFormat(pattern, Locale.getDefault())
        return formatter.format(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get references to UI elements
        val currencyEditText = binding.amountEditText
        val convertButton = binding.convertButton
        val resultTextView = binding.resultTextView
        initialExchangeRate(this)

        // Initialize InputMethodManager
        imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager

        // Populate the spinners with currencies
        ArrayAdapter(this, android.R.layout.simple_spinner_item, currencies).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.fromCurrencySpinner.adapter = adapter
            binding.toCurrencySpinner.adapter = adapter
        }


        convertButton.setOnClickListener {
            // Hide the keyboard
            imm.hideSoftInputFromWindow(currencyEditText.windowToken, 0)

            // Get currency amount from the EditText
            val currencyAmount = currencyEditText.text.toString().toDoubleOrNull()

            // If the input is not a valid number, show an error message and return
            if (currencyAmount == null) {
                Snackbar.make(binding.root, "Please enter a valid number", Snackbar.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            // Get the selected currencies
            val fromCurrency =
                binding.fromCurrencySpinner.selectedItem.toString().substring(0, 3)
            val toCurrency = binding.toCurrencySpinner.selectedItem.toString().substring(0, 3)

            // Convert the currency using the exchange rate API
            fetchExchangeRate(
                fromCurrency,
                toCurrency,
                currencyAmount,
                onSuccess = { convertedAmount, toCurrency ->
                    // Show the converted amount in the resultTextView
                    val result = "$convertedAmount $toCurrency"
                    resultTextView.text = result
                },
                onError = { error ->
                    // Show an error message in a Snackbar
                    Snackbar.make(binding.root, "Error: $error", Snackbar.LENGTH_SHORT).show()
                },
                onFetchComplete = { message ->
                    // Show the fetch completion message in a Snackbar
                    Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
                },
                context = this // pass the current context here
            )
        }

        // Set the title of the app bar
        supportActionBar?.title = "Currency Converter"
    }



    private fun fetchExchangeRate(
        fromCurrency: String,
        toCurrency: String,
        amount: Double,
        onSuccess: (Double, String) -> Unit,
        onError: (String) -> Unit,
        onFetchComplete: (String) -> Unit,
        context: Context
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val url = "https://api.exchangerate-api.com/v4/latest/$fromCurrency"
            try {
                // 檢查網路連接
                val connectivityManager =
                    context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val networkInfo = connectivityManager.activeNetworkInfo
                if (networkInfo != null && networkInfo.isConnected) {
                    // 如果有網路連接，從網路獲取匯率
                    val apiResult = URL(url).readText()
                    val jsonObject = JSONObject(apiResult)
                    val rates = jsonObject.getJSONObject("rates")
                    val fromRate = rates.getDouble(fromCurrency)
                    val toRate = rates.getDouble(toCurrency)
                    val convertedAmount = amount / fromRate * toRate
                    val result = "$convertedAmount $toCurrency"
                    Log.d("MainActivity", "$amount $fromCurrency = $result")
                    withContext(Dispatchers.Main) {
                        onSuccess(convertedAmount, toCurrency)
                        val updateTime = Date().toSimpleString("yyyy-MM-dd HH:mm:ss")
                        buildTime = updateTime
                        onFetchComplete("Last updated: $updateTime")
                    }
                } else {
                    // 如果沒有網路連接，從本地文件獲取匯率
                    try {
                        val file = File(context.filesDir, "exchange_rates.json")
                        val json = file.readText()
                        val jsonObject = JSONObject(json)
                        val rates = jsonObject.getJSONObject("rates")
                        val fromRate = rates.getDouble(fromCurrency)
                        val toRate = rates.getDouble(toCurrency)
                        val convertedAmount = amount / fromRate * toRate
                        val result = "$convertedAmount $toCurrency"

                        buildTime = jsonObject.getString("date")

                        Log.d("MainActivity", "$amount $fromCurrency = $result")
                        withContext(Dispatchers.Main) {
                            onSuccess(convertedAmount, toCurrency)
                            onFetchComplete("Last updated(沒有網路連線):$buildTime")
                        }
                    } catch (e: FileNotFoundException) {
                        // 文件不存在，从assets中读取文件
                        Log.e("MainActivity", e.toString())
                        withContext(Dispatchers.Main) {
                            onError("File not found")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", e.toString())
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "Unknown error")
                }
            }
        }
    }




    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val networkInfo = connectivityManager?.activeNetworkInfo
        return networkInfo?.isConnected ?: false
    }

    private fun readJsonFromAsset(fileName: String): String? {
        return try {
            val inputStream = assets.open(fileName)
            val buffer = ByteArray(inputStream.available())
            inputStream.read(buffer)
            inputStream.close()
            String(buffer, Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }


    private fun initialExchangeRate(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val url = "https://api.exchangerate-api.com/v4/latest/TWD"
            try {
                val apiResult = URL(url).readText()
                val jsonObject = JSONObject(apiResult)
                buildTime = jsonObject.getString("date")
                // 將數據寫入本地文件
                val file = File(context.filesDir, "exchange_rates.json")
                file.writeText(apiResult)
            } catch (e: Exception) {
                Log.e("MainActivity", e.toString())
                withContext(Dispatchers.Main) {
                }
            }
        }
    }






}
