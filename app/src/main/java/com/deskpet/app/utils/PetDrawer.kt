package com.deskpet.app.utils

import android.graphics.*
import kotlin.math.*

/**
 * 宠物绘制器 — 纯 Canvas 绘制一只可爱的猫咪。
 * 所有坐标基于中心点为 (0,0)，size 为基准缩放。
 */
object PetDrawer {

    // ─── 颜色常量 ───
    private val COLOR_BODY = Color.parseColor("#FFE4C4")      // 米白身体
    private val COLOR_DARK = Color.parseColor("#D2956A")      // 深色条纹
    private val COLOR_EAR_INNER = Color.parseColor("#FFD5C2") // 耳朵内侧
    private val COLOR_EYE = Color.parseColor("#2C3E50")       // 眼睛
    private val COLOR_EYE_HIGHLIGHT = Color.parseColor("#FFFFFF")
    private val COLOR_NOSE = Color.parseColor("#FFB3B3")      // 鼻子
    private val COLOR_BLUSH = Color.parseColor("#FFB3B3")     // 腮红
    private val COLOR_PAW = Color.parseColor("#FFE4C4")       // 爪子
    private val COLOR_MOUTH = Color.parseColor("#5D4E37")
    private val COLOR_TONGUE = Color.parseColor("#FF8A8A")
    private val COLOR_ZZZ = Color.parseColor("#A0C4FF")

    // 画笔缓存
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    /**
     * 绘制宠物。
     * @param canvas 画布
     * @param cx 宠物中心 X
     * @param cy 宠物中心 Y
     * @param size 宠物大小（直径基准）
     * @param mood 当前表情 mood：idle/happy/eat/love/sleep/tired/hungry/sad/cry
     * @param animProgress 动画进度 [0, 1]，用于弹跳等
     */
    fun draw(canvas: Canvas, cx: Float, cy: Float, size: Float, mood: String, animProgress: Float) {
        val s = size / 2f // 半径
        canvas.save()
        canvas.translate(cx, cy)

        // 整体弹跳偏移
        val bounce = when (mood) {
            "happy" -> sin(animProgress * PI * 4f).toFloat() * s * 0.15f
            "love" -> sin(animProgress * PI * 3f).toFloat() * s * 0.08f
            else -> sin(animProgress * PI * 2f).toFloat() * s * 0.04f
        }
        canvas.translate(0f, -abs(bounce))

        drawBody(canvas, s, mood, animProgress)
        drawEars(canvas, s, mood, animProgress)
        drawFace(canvas, s, mood, animProgress)
        drawPaws(canvas, s, mood, animProgress)
        drawTail(canvas, s)

        if (mood == "sleep") drawZzz(canvas, s, animProgress)

        canvas.restore()
    }

    /** 身体：椭圆形，带条纹 */
    private fun drawBody(canvas: Canvas, s: Float, mood: String, progress: Float) {
        val bodyRect = RectF(-s * 0.65f, -s * 0.5f, s * 0.65f, s * 0.55f)

        // 身体有点微微缩放呼吸效果
        val scaleX = 1f + sin(progress * PI * 2).toFloat() * 0.03f
        val scaleY = 1f + cos(progress * PI * 2).toFloat() * 0.03f
        canvas.save()
        canvas.scale(scaleX, scaleY)

        fillPaint.color = COLOR_BODY
        canvas.drawOval(bodyRect, fillPaint)

        // 肚皮浅色
        val bellyRect = RectF(-s * 0.35f, -s * 0.15f, s * 0.35f, s * 0.45f)
        fillPaint.color = Color.argb(80, 255, 255, 255)
        canvas.drawOval(bellyRect, fillPaint)

        // 头顶深色条纹
        fillPaint.color = COLOR_DARK
        canvas.drawArc(
            RectF(-s * 0.3f, -s * 0.55f, s * 0.3f, -s * 0.2f),
            180f, 180f, true, fillPaint
        )

        // 两侧条纹
        for (i in -1..1 step 2) {
            fillPaint.color = Color.argb(60, 210, 149, 106)
            canvas.drawOval(
                RectF(i * s * 0.2f, -s * 0.2f, i * s * 0.55f, s * 0.3f),
                fillPaint
            )
        }

        canvas.restore()
    }

    /** 耳朵 */
    private fun drawEars(canvas: Canvas, s: Float, mood: String, progress: Float) {
        val earWiggle = if (mood == "happy") sin(progress * PI * 6).toFloat() * 3f else 0f

        for (side in -1..1 step 2) {
            val baseX = side * s * 0.45f
            val baseY = -s * 0.45f
            val tipX = side * s * 0.32f + earWiggle * side
            val tipY = -s * 0.95f

            val earPath = Path().apply {
                moveTo(baseX - side * s * 0.15f, baseY)
                lineTo(tipX, tipY)
                lineTo(baseX + side * s * 0.25f, baseY)
                close()
            }
            fillPaint.color = COLOR_BODY
            canvas.drawPath(earPath, fillPaint)

            // 内侧
            val innerPath = Path().apply {
                moveTo(baseX - side * s * 0.08f, baseY - s * 0.03f)
                lineTo(tipX, tipY + s * 0.1f)
                lineTo(baseX + side * s * 0.15f, baseY - s * 0.03f)
                close()
            }
            fillPaint.color = COLOR_EAR_INNER
            canvas.drawPath(innerPath, fillPaint)
        }
    }

    /** 脸部：眼睛、鼻子、嘴巴、腮红 */
    private fun drawFace(canvas: Canvas, s: Float, mood: String, progress: Float) {
        val eyeY = -s * 0.08f
        val eyeSpacing = s * 0.22f

        // ── 眼睛 ──
        for (side in -1..1 step 2) {
            val ex = side * eyeSpacing

            when (mood) {
                "cry" -> {
                    // T_T 哭眼
                    fillPaint.color = COLOR_EYE
                    canvas.drawCircle(ex, eyeY, s * 0.06f, fillPaint)
                    // 眼泪
                    fillPaint.color = Color.parseColor("#89CFF0")
                    val tearY = eyeY + s * 0.12f + sin(progress * PI * 3).toFloat() * s * 0.04f
                    canvas.drawOval(ex - s * 0.03f, tearY, ex + s * 0.03f, tearY + s * 0.08f, fillPaint)
                }
                "sleep" -> {
                    // -_- 闭眼
                    strokePaint.color = COLOR_EYE
                    strokePaint.strokeWidth = s * 0.03f
                    canvas.drawLine(ex - s * 0.07f, eyeY, ex + s * 0.07f, eyeY, strokePaint)
                    strokePaint.strokeWidth = 2f
                }
                "happy", "love" -> {
                    // ^^ 开心眼 (弧线)
                    strokePaint.color = COLOR_EYE
                    strokePaint.strokeWidth = s * 0.025f
                    val eyePath = Path().apply {
                        moveTo(ex - s * 0.07f, eyeY)
                        quadTo(ex, eyeY - s * 0.08f, ex + s * 0.07f, eyeY)
                    }
                    canvas.drawPath(eyePath, strokePaint)
                    strokePaint.strokeWidth = 2f
                }
                "tired", "hungry", "sad" -> {
                    // 半闭眼
                    fillPaint.color = COLOR_EYE
                    canvas.drawOval(ex - s * 0.06f, eyeY - s * 0.06f, ex + s * 0.06f, eyeY + s * 0.01f, fillPaint)
                }
                else -> {
                    // 正常圆眼
                    fillPaint.color = Color.WHITE
                    canvas.drawOval(ex - s * 0.07f, eyeY - s * 0.09f, ex + s * 0.07f, eyeY + s * 0.07f, fillPaint)
                    fillPaint.color = COLOR_EYE
                    canvas.drawCircle(ex, eyeY - s * 0.01f, s * 0.055f, fillPaint)
                    // 高光
                    fillPaint.color = COLOR_EYE_HIGHLIGHT
                    canvas.drawCircle(ex + s * 0.02f, eyeY - s * 0.03f, s * 0.02f, fillPaint)
                }
            }
        }

        // ── 鼻子 ──
        val noseY = s * 0.12f
        fillPaint.color = COLOR_NOSE
        val nosePath = Path().apply {
            moveTo(0f, noseY - s * 0.04f)
            lineTo(-s * 0.04f, noseY + s * 0.02f)
            lineTo(s * 0.04f, noseY + s * 0.02f)
            close()
        }
        canvas.drawPath(nosePath, fillPaint)

        // ── 嘴巴 ──
        strokePaint.color = COLOR_MOUTH
        strokePaint.strokeWidth = s * 0.02f
        when (mood) {
            "happy", "love" -> {
                // 张大嘴笑
                val mouthY = noseY + s * 0.06f
                fillPaint.color = Color.parseColor("#FF6B6B")
                canvas.drawOval(-s * 0.08f, mouthY, s * 0.08f, mouthY + s * 0.1f, fillPaint)
                fillPaint.color = COLOR_TONGUE
                canvas.drawOval(-s * 0.04f, mouthY + s * 0.04f, s * 0.04f, mouthY + s * 0.09f, fillPaint)
            }
            "cry", "sad" -> {
                // 撇嘴
                val mouthPath = Path().apply {
                    moveTo(-s * 0.07f, noseY + s * 0.1f)
                    quadTo(0f, noseY + s * 0.04f, s * 0.07f, noseY + s * 0.1f)
                }
                canvas.drawPath(mouthPath, strokePaint)
            }
            "tired" -> {
                canvas.drawLine(-s * 0.05f, noseY + s * 0.07f, s * 0.05f, noseY + s * 0.07f, strokePaint)
            }
            else -> {
                // 正常 w 嘴
                val mouthPath = Path().apply {
                    moveTo(-s * 0.07f, noseY + s * 0.06f)
                    quadTo(-s * 0.03f, noseY + s * 0.11f, 0f, noseY + s * 0.06f)
                    quadTo(s * 0.03f, noseY + s * 0.11f, s * 0.07f, noseY + s * 0.06f)
                }
                canvas.drawPath(mouthPath, strokePaint)
            }
        }
        strokePaint.strokeWidth = 2f

        // ── 腮红 ──
        fillPaint.color = Color.argb(
            if (mood == "love") 160 else 80,
            255, 179, 179
        )
        canvas.drawOval(-s * 0.4f, eyeY + s * 0.04f, -s * 0.22f, eyeY + s * 0.14f, fillPaint)
        canvas.drawOval(s * 0.22f, eyeY + s * 0.04f, s * 0.4f, eyeY + s * 0.14f, fillPaint)
    }

    /** 小爪子 */
    private fun drawPaws(canvas: Canvas, s: Float, mood: String, progress: Float) {
        fillPaint.color = COLOR_PAW
        val pawY = s * 0.4f
        val pawSpread = s * 0.5f

        for (side in -1..1 step 2) {
            val px = side * pawSpread
            canvas.drawOval(px - s * 0.12f, pawY - s * 0.06f, px + s * 0.12f, pawY + s * 0.12f, fillPaint)

            // 小肉垫线
            strokePaint.color = COLOR_DARK
            strokePaint.strokeWidth = s * 0.015f
            canvas.drawLine(px, pawY, px, pawY + s * 0.08f, strokePaint)
            canvas.drawLine(px - s * 0.04f, pawY + s * 0.04f, px + s * 0.04f, pawY + s * 0.04f, strokePaint)
            strokePaint.strokeWidth = 2f
        }

        // 吃东西时抬起一只爪子
        if (mood == "eat") {
            val wave = sin(progress * PI * 5).toFloat() * s * 0.1f
            fillPaint.color = COLOR_PAW
            canvas.drawOval(
                s * 0.35f, -s * 0.1f + wave,
                s * 0.55f, s * 0.08f + wave,
                fillPaint
            )
        }
    }

    /** 尾巴 */
    private fun drawTail(canvas: Canvas, s: Float) {
        val tailPath = Path().apply {
            moveTo(s * 0.5f, s * 0.2f)
            quadTo(s * 0.8f, s * 0.1f, s * 0.75f, -s * 0.15f)
            quadTo(s * 0.7f, -s * 0.35f, s * 0.85f, -s * 0.5f)
        }
        strokePaint.color = COLOR_DARK
        strokePaint.strokeWidth = s * 0.06f
        strokePaint.strokeCap = Paint.Cap.ROUND
        canvas.drawPath(tailPath, strokePaint)
        strokePaint.strokeWidth = 2f
    }

    /** Zzz 睡觉符号 */
    private fun drawZzz(canvas: Canvas, s: Float, progress: Float) {
        fillPaint.color = COLOR_ZZZ
        fillPaint.textSize = s * 0.25f
        fillPaint.isFakeBoldText = true

        val zzz = listOf("z", "Z", "Z")
        for ((i, ch) in zzz.withIndex()) {
            val alpha = ((sin(progress * PI * 2 + i * 1.2).toFloat() + 1f) / 2f * 200 + 55).toInt()
            fillPaint.alpha = alpha
            val tx = s * 0.5f + i * s * 0.12f
            val ty = -s * 0.5f - i * s * 0.18f - progress * s * 0.1f
            canvas.drawText(ch, tx, ty, fillPaint)
        }
        fillPaint.alpha = 255
    }
}