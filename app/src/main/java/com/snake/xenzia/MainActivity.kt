package com.snake.xenzia

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.TextView

class MainActivity : Activity() {
    
    private lateinit var gameView: GameView
    private lateinit var scoreText: TextView
    private lateinit var highScoreText: TextView
    private lateinit var restartButton: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        gameView = findViewById(R.id.gameView)
        scoreText = findViewById(R.id.scoreText)
        highScoreText = findViewById(R.id.highScoreText)
        restartButton = findViewById(R.id.restartButton)
        
        val highScore = getSharedPreferences("snake_prefs", Context.MODE_PRIVATE)
            .getInt("high_score", 0)
        highScoreText.text = "High Score: $highScore"
        
        restartButton.setOnClickListener {
            gameView.restartGame()
        }
        
        gameView.setScoreListener(object : GameView.ScoreListener {
            override fun onScoreUpdate(score: Int, highScore: Int) {
                scoreText.text = "Score: $score"
                highScoreText.text = "High Score: $highScore"
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
    private val gridPaint = Paint().apply { color = Color.DKGRAY }
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
    
    fun startGame() {
        snake = mutableListOf(Pair(10, 10), Pair(9, 10), Pair(8, 10))
        direction = "right"
        nextDirection = "right"
        score = 0
        isRunning = true
        isPaused = false
        spawnFood()
        scoreListener?.onScoreUpdate(score, highScore)
        handler.post(gameRunnable)
    }
    
    fun pauseGame() {
        isPaused = true
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
        
        canvas.drawColor(Color.BLACK)
        
        for (i in 0 until gridSize) {
            for (j in 0 until gridSize) {
                canvas.drawRect(
                    i * cellWidth, j * cellHeight,
                    (i + 1) * cellWidth, (j + 1) * cellHeight,
                    gridPaint
                )
            }
        }
        
        canvas.drawCircle(
            food.first * cellWidth + cellWidth / 2,
            food.second * cellHeight + cellHeight / 2,
            cellWidth / 3,
            foodPaint
        )
        
        for (i in snake.indices) {
            val segment = snake[i]
            paint.color = if (i == 0) Color.YELLOW else Color.GREEN
            canvas.drawRect(
                segment.first * cellWidth + 2,
                segment.second * cellHeight + 2,
                (segment.first + 1) * cellWidth - 2,
                (segment.second + 1) * cellHeight - 2,
                paint
            )
        }
        
        if (!isRunning && snake.isNotEmpty()) {
            val gameOverPaint = Paint().apply {
                color = Color.WHITE
                textSize = 60f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("GAME OVER", width / 2, height / 2, gameOverPaint)
        }
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (!isRunning) {
                    startGame()
                    return true
                }
                
                val x = event.x
                val y = event.y
                val dx = x - width / 2f
                val dy = y - height / 2f
                
                if (Math.abs(dx) > Math.abs(dy)) {
                    nextDirection = if (dx > 0) "right" else "left"
                } else {
                    nextDirection = if (dy > 0) "down" else "up"
                }
                
                if ((direction == "right" && nextDirection != "left") ||
                    (direction == "left" && nextDirection != "right") ||
                    (direction == "up" && nextDirection != "down") ||
                    (direction == "down" && nextDirection != "up")) {
                    direction = nextDirection
                }
            }
        }
        return true
    }
}
