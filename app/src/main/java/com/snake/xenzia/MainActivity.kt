package com.snake.xenzia

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView

class MainActivity : Activity() {
    
    private lateinit var gameView: GameView
    private lateinit var scoreText: TextView
    private lateinit var highScoreText: TextView
    private lateinit var restartButton: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        scoreText = findViewById(R.id.scoreText)
        highScoreText = findViewById(R.id.highScoreText)
        restartButton = findViewById(R.id.restartButton)
        val gameContainer = findViewById<FrameLayout>(R.id.gameContainer)
        
        gameView = GameView(this)
        gameContainer.addView(gameView)
        
        val highScore = getSharedPreferences("snake_prefs", Context.MODE_PRIVATE)
            .getInt("high_score", 0)
        highScoreText.text = "High: $highScore"
        
        // D-pad buttons
        findViewById<Button>(R.id.btnUp).setOnClickListener {
            gameView.changeDirection("up")
        }
        findViewById<Button>(R.id.btnDown).setOnClickListener {
            gameView.changeDirection("down")
        }
        findViewById<Button>(R.id.btnLeft).setOnClickListener {
            gameView.changeDirection("left")
        }
        findViewById<Button>(R.id.btnRight).setOnClickListener {
            gameView.changeDirection("right")
        }
        findViewById<Button>(R.id.btnCenter).setOnClickListener {
            if (!gameView.isGameRunning()) {
                gameView.startGame()
            }
        }
        
        // A button = Restart
        findViewById<Button>(R.id.btnA).setOnClickListener {
            gameView.restartGame()
        }
        
        // B button = Start/Pause
        findViewById<Button>(R.id.btnB).setOnClickListener {
            if (gameView.isGameRunning()) {
                gameView.pauseGame()
            } else {
                gameView.startGame()
            }
        }
        
        restartButton.setOnClickListener {
            gameView.restartGame()
        }
        
        gameView.setScoreListener(object : GameView.ScoreListener {
            override fun onScoreUpdate(score: Int, highScore: Int) {
                scoreText.text = "Score: $score"
                highScoreText.text = "High: $highScore"
            }
        })
    }
    
    override fun onPause() {
        super.onPause()
        gameView.pauseGame()
    }
    
    override fun onResume() {
        super.onResume()
        gameView.resumeGame()
    }
}

class GameView(context: Context) : View(context) {
    
    interface ScoreListener {
        fun onScoreUpdate(score: Int, highScore: Int)
    }
    
    private val gridSize = 20
    private var snake = mutableListOf(Pair(10, 10), Pair(9, 10), Pair(8, 10))
    private var direction = "right"
    private var nextDirection = "right"
    private var food = Pair(15, 10)
    private var score = 0
    private var highScore = 0
    private var isRunning = false
    private var isPaused = false
    private val handler = Handler(Looper.getMainLooper())
    private val paint = Paint()
    private val foodPaint = Paint().apply { color = Color.RED }
    private val gridPaint = Paint().apply {
        color = Color.parseColor("#8FBC3A")
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }
    private var scoreListener: ScoreListener? = null
    
    init {
        val prefs = context.getSharedPreferences("snake_prefs", Context.MODE_PRIVATE)
        highScore = prefs.getInt("high_score", 0)
    }
    
    private val gameRunnable = object : Runnable {
        override fun run() {
            if (isRunning && !isPaused) {
                moveSnake()
                invalidate()
                val speed = Math.max(100, 300 - (score * 5))
                handler.postDelayed(this, speed.toLong())
            }
        }
    }
    
    fun setScoreListener(listener: ScoreListener) {
        scoreListener = listener
    }
    
    fun changeDirection(newDirection: String) {
        if ((direction == "right" && newDirection != "left") ||
            (direction == "left" && newDirection != "right") ||
            (direction == "up" && newDirection != "down") ||
            (direction == "down" && newDirection != "up")) {
            nextDirection = newDirection
        }
    }
    
    fun isGameRunning(): Boolean {
        return isRunning
    }
    
    fun startGame() {
        snake = mutableListOf(Pair(10, 10), Pair(9, 10), Pair(8, 10))
        direction = "right"
        nextDirection = "right"
        score = 0
        isRunning = true
        isPaused = false
        spawnFood()
        scoreListener?.onScoreUpdate(score, highScore)
        handler.removeCallbacks(gameRunnable)
        handler.post(gameRunnable)
    }
    
    fun pauseGame() {
        isPaused = true
        handler.removeCallbacks(gameRunnable)
    }
    
    fun resumeGame() {
        if (isRunning) {
            isPaused = false
            handler.post(gameRunnable)
        }
    }
    
    fun restartGame() {
        handler.removeCallbacks(gameRunnable)
        startGame()
    }
    
    private fun moveSnake() {
        direction = nextDirection
        val head = snake.first()
        val newHead = when (direction) {
            "up" -> Pair(head.first, head.second - 1)
            "down" -> Pair(head.first, head.second + 1)
            "left" -> Pair(head.first - 1, head.second)
            "right" -> Pair(head.first + 1, head.second)
            else -> head
        }
        
        if (newHead.first < 0 || newHead.first >= gridSize || 
            newHead.second < 0 || newHead.second >= gridSize) {
            gameOver()
            return
        }
        
        if (snake.contains(newHead)) {
            gameOver()
            return
        }
        
        snake.add(0, newHead)
        
        if (newHead == food) {
            score++
            if (score > highScore) {
                highScore = score
                context.getSharedPreferences("snake_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putInt("high_score", highScore)
                    .apply()
            }
            scoreListener?.onScoreUpdate(score, highScore)
            spawnFood()
        } else {
            snake.removeAt(snake.size - 1)
        }
    }
    
    private fun spawnFood() {
        var newFood: Pair<Int, Int>
        do {
            newFood = Pair((0 until gridSize).random(), (0 until gridSize).random())
        } while (snake.contains(newFood))
        food = newFood
    }
    
    private fun gameOver() {
        isRunning = false
        handler.removeCallbacks(gameRunnable)
        invalidate()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val width = width.toFloat()
        val height = height.toFloat()
        val cellWidth = width / gridSize
        val cellHeight = height / gridSize
        
        // Nokia green background
        canvas.drawColor(Color.parseColor("#9BBC0F"))
        
        // Draw grid lines
        for (i in 0..gridSize) {
            canvas.drawLine(
                i * cellWidth, 0f,
                i * cellWidth, height,
                gridPaint
            )
            canvas.drawLine(
                0f, i * cellHeight,
                width, i * cellHeight,
                gridPaint
            )
        }
        
        // Draw food (dark green for Nokia feel)
        foodPaint.color = Color.parseColor("#0F380F")
        canvas.drawCircle(
            food.first * cellWidth + cellWidth / 2,
            food.second * cellHeight + cellHeight / 2,
            cellWidth / 3,
            foodPaint
        )
        
        // Draw snake
        for (i in snake.indices) {
            val segment = snake[i]
            paint.color = if (i == 0) {
                Color.parseColor("#306230")
            } else {
                Color.parseColor("#0F380F")
            }
            canvas.drawRect(
                segment.first * cellWidth + 2,
                segment.second * cellHeight + 2,
                (segment.first + 1) * cellWidth - 2,
                (segment.second + 1) * cellHeight - 2,
                paint
            )
        }
        
        // Draw game over or start text
        if (!isRunning && snake.isNotEmpty()) {
            val gameOverPaint = Paint().apply {
                color = Color.parseColor("#0F380F")
                textSize = 50f
                textAlign = Paint.Align.CENTER
                typeface = android.graphics.Typeface.create("monospace", android.graphics.Typeface.BOLD)
            }
            canvas.drawText("GAME OVER", width / 2, height / 2, gameOverPaint)
            canvas.drawText("Press START", width / 2, height / 2 + 50, gameOverPaint)
        } else if (!isRunning && snake.isEmpty()) {
            val startPaint = Paint().apply {
                color = Color.parseColor("#0F380F")
                textSize = 40f
                textAlign = Paint.Align.CENTER
                typeface = android.graphics.Typeface.create("monospace", android.graphics.Typeface.BOLD)
            }
            canvas.drawText("SNAKE XENZIA", width / 2, height / 2, startPaint)
            canvas.drawText("Press START", width / 2, height / 2 + 50, startPaint)
        }
    }
}
