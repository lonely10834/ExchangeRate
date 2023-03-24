package com.example.myapp01

import android.annotation.SuppressLint
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import com.example.myapp01.databinding.ActivityMainBinding
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import android.content.Context
import android.content.res.Configuration
import android.graphics.drawable.GradientDrawable
import android.view.inputmethod.InputMethodManager
import java.util.Date
import java.text.SimpleDateFormat
import java.util.*
import java.io.File
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import java.io.FileNotFoundException



class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var editText: EditText
    // Define a list of currencies to support
    private val currencies = arrayOf("TWD", "USD", "JPY", "CAD")
    private var buildTime: String? = null
    // Declare a lateinit variable for InputMethodManager
    private lateinit var imm: InputMethodManager
    private fun Date.toSimpleString(pattern: String): String {
        val formatter = SimpleDateFormat(pattern, Locale.getDefault())
        return formatter.format(this)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val configuration = resources.configuration
        onConfigurationChanged(configuration)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO) //強制淺色模式

        // Get references to UI elements
        val currencyEditText = binding.amountEditText
        val convertButton = binding.convertButton
        val resultTextView = binding.resultTextView
        val resultTextView01 = binding.resultTextView01
        val swapButton = findViewById<Button>(R.id.swap_button)

        initialExchangeRate(this)

        // Initialize InputMethodManager
        imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager


        // Populate the spinners with currencies
        ArrayAdapter(this, android.R.layout.simple_spinner_item, currencies).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.fromCurrencySpinner.adapter = adapter
            binding.toCurrencySpinner.adapter = adapter
        }

        binding.fromCurrencySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCurrency = currencies[position]
                binding.amountEditText.hint = getString(R.string.enter_amount_in, selectedCurrency)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }



        //按下交換按鈕
        swapButton.setOnClickListener {
            swapSpinnerItems()
            val spinnerFromCurrency = binding.fromCurrencySpinner.selectedItem
            val spinnerToCurrency = binding.toCurrencySpinner.selectedItem
            binding.fromCurrencySpinner.setSelection(currencies.indexOf(spinnerToCurrency))
            binding.toCurrencySpinner.setSelection(currencies.indexOf(spinnerFromCurrency))
            // Get currency amount from the EditText
            val currencyAmount = currencyEditText.text.toString().toDoubleOrNull()
            // If the input is not a valid number, show an error message and return
            if (currencyAmount == null) {
                Snackbar.make(binding.root, "Please enter a valid number", Snackbar.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }
            fetchExchangeRate(
                currencyAmount,
                onSuccess = { convertedAmount,fromCurrency, toCurrency, toRate ->
                    // Show the converted amount in the resultTextView
                    val result = "$convertedAmount $toCurrency"
                    resultTextView.text = result
                    resultTextView01.text = "1 $fromCurrency = $toRate $toCurrency"
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
            resultTextView01.text = getString(R.string.exchange_rate_result, spinnerFromCurrency, spinnerToCurrency)
        }
        //按下轉換按鈕
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

            // Convert the currency using the exchange rate API
            fetchExchangeRate(
                currencyAmount,
                onSuccess = { convertedAmount,fromCurrency, toCurrency, toRate ->
                    // Show the converted amount in the resultTextView
                    val result = "$convertedAmount $toCurrency"
                    resultTextView.text = result
                    resultTextView01.text = "1 $fromCurrency = $toRate $toCurrency"
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

    private fun swapSpinnerItems() {
        val fromSpinner = binding.fromCurrencySpinner
        val toSpinner = binding.toCurrencySpinner

        val fromIndex = fromSpinner.selectedItemPosition
        val toIndex = toSpinner.selectedItemPosition

        // Swap the selected items
        val temp = currencies[fromIndex]
        currencies[fromIndex] = currencies[toIndex]
        currencies[toIndex] = temp

        // Update the spinner adapters
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, currencies)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        fromSpinner.adapter = adapter
        toSpinner.adapter = adapter

        // Set the selected items back to their original positions
        fromSpinner.setSelection(toIndex, true)
        toSpinner.setSelection(fromIndex, true)
    }

private fun updateBorderColor() {
    val isDarkMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    val borderColorResId = if (isDarkMode) R.color.border_color_light else R.color.border_color_dark
    val borderColorStateList = ContextCompat.getColorStateList(this, borderColorResId)
    val borderDrawable = ContextCompat.getDrawable(this, R.drawable.border)
    borderDrawable?.let { drawable ->
        if (drawable is GradientDrawable) {
            drawable.setStroke(2, borderColorStateList)
            drawable.state = intArrayOf(android.R.attr.state_enabled)
        }
    }
    editText.isEnabled = !isDarkMode
}


    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        // 獲取當前的深色模式或淺色模式
        var currentUiMode = newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK

        // 使用者改變深色模式與否
        if (newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK != currentUiMode) {
            currentUiMode = newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK
            val isDarkMode = currentUiMode == Configuration.UI_MODE_NIGHT_YES
            updateBorderColor()
        }
    }


    private fun fetchExchangeRate(
//        fromCurrency: String,
//        toCurrency: String,
        amount: Double,
        onSuccess: (Double,String, String, Double) -> Unit,
        onError: (String) -> Unit,
        onFetchComplete: (String) -> Unit,
        context: Context
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            // Get the selected currencies
            val fromCurrency =
                binding.fromCurrencySpinner.selectedItem.toString().substring(0, 3)
            val toCurrency = binding.toCurrencySpinner.selectedItem.toString().substring(0, 3)
            val url = "https://api.exchangerate-api.com/v4/latest/$fromCurrency"
            try {
                // 檢查網路連接
                val connectivityManager =
                    context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val network = connectivityManager.activeNetwork
                val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
                if (networkCapabilities != null && networkCapabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
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
                        onSuccess(convertedAmount,fromCurrency, toCurrency , toRate)
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
                            onSuccess(convertedAmount,fromCurrency, toCurrency,toRate)
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
