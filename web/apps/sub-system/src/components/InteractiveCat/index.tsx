import React, { useState, useEffect, useRef, useCallback } from 'react';
import styles from './InteractiveCat.module.less';

interface InteractiveCatProps {
  /** 是否正在输入（触发遮眼动画） */
  isTyping?: boolean;
  /** 小猫尺寸 */
  size?: number;
}

const InteractiveCat: React.FC<InteractiveCatProps> = ({
  isTyping = false,
  size = 160,
}) => {
  const [mousePosition, setMousePosition] = useState({ x: 0, y: 0 });
  const [eyeOffset, setEyeOffset] = useState({ x: 0, y: 0 });
  const catRef = useRef<HTMLDivElement>(null);
  const animationRef = useRef<number>();

  useEffect(() => {
    const handleMouseMove = (e: MouseEvent) => {
      setMousePosition({ x: e.clientX, y: e.clientY });
    };

    window.addEventListener('mousemove', handleMouseMove);
    return () => window.removeEventListener('mousemove', handleMouseMove);
  }, []);

  const calculateEyeOffset = useCallback(() => {
    if (!catRef.current) return { x: 0, y: 0 };

    const rect = catRef.current.getBoundingClientRect();
    const catCenterX = rect.left + rect.width / 2;
    const catCenterY = rect.top + rect.height / 2 - 30;

    const deltaX = mousePosition.x - catCenterX;
    const deltaY = mousePosition.y - catCenterY;

    const maxOffset = 6;
    const distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    const normalizedDistance = Math.min(distance / 300, 1);

    return {
      x: (deltaX / (distance || 1)) * maxOffset * normalizedDistance,
      y: (deltaY / (distance || 1)) * maxOffset * normalizedDistance,
    };
  }, [mousePosition]);

  useEffect(() => {
    const animate = () => {
      const targetOffset = calculateEyeOffset();
      setEyeOffset((prev) => ({
        x: prev.x + (targetOffset.x - prev.x) * 0.12,
        y: prev.y + (targetOffset.y - prev.y) * 0.12,
      }));
      animationRef.current = requestAnimationFrame(animate);
    };

    animationRef.current = requestAnimationFrame(animate);
    return () => {
      if (animationRef.current) {
        cancelAnimationFrame(animationRef.current);
      }
    };
  }, [calculateEyeOffset]);

  return (
    <div
      ref={catRef}
      className={styles.catContainer}
      style={{ width: size, height: size }}
    >
      <svg
        viewBox="0 0 200 200"
        className={styles.catSvg}
        style={{ width: size, height: size }}
      >
        <defs>
          {/* 橘猫毛色渐变 */}
          <linearGradient id="furGradient" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor="#FFB86C" />
            <stop offset="50%" stopColor="#FFA54F" />
            <stop offset="100%" stopColor="#FF9632" />
          </linearGradient>

          {/* 浅色毛发 */}
          <linearGradient id="lightFur" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor="#FFE4C4" />
            <stop offset="100%" stopColor="#FFDAB9" />
          </linearGradient>

          {/* 耳朵内部 */}
          <linearGradient id="innerEar" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor="#FFB5B5" />
            <stop offset="100%" stopColor="#FF9999" />
          </linearGradient>

          {/* 鼻子 */}
          <linearGradient id="noseColor" x1="0%" y1="0%" x2="0%" y2="100%">
            <stop offset="0%" stopColor="#FF9999" />
            <stop offset="100%" stopColor="#FF7777" />
          </linearGradient>

          {/* 阴影 */}
          <filter id="shadow" x="-30%" y="-30%" width="160%" height="160%">
            <feDropShadow dx="0" dy="4" stdDeviation="6" floodColor="#000" floodOpacity="0.15" />
          </filter>

          {/* 条纹图案 */}
          <pattern id="stripes" patternUnits="userSpaceOnUse" width="20" height="20" patternTransform="rotate(15)">
            <line x1="0" y1="0" x2="0" y2="20" stroke="#E88A3C" strokeWidth="3" opacity="0.3" />
          </pattern>
        </defs>

        {/* 尾巴 */}
        <g className={styles.tail}>
          <path
            d="M155 140 Q175 125 172 95 Q168 65 185 50"
            fill="none"
            stroke="url(#furGradient)"
            strokeWidth="14"
            strokeLinecap="round"
          />
          {/* 尾巴条纹 */}
          <path
            d="M155 140 Q175 125 172 95 Q168 65 185 50"
            fill="none"
            stroke="url(#stripes)"
            strokeWidth="14"
            strokeLinecap="round"
          />
        </g>

        {/* 身体 */}
        <g className={styles.body}>
          <ellipse
            cx="100"
            cy="155"
            rx="50"
            ry="38"
            fill="url(#furGradient)"
            filter="url(#shadow)"
          />
          {/* 身体条纹 */}
          <ellipse
            cx="100"
            cy="155"
            rx="50"
            ry="38"
            fill="url(#stripes)"
          />
          {/* 肚子 */}
          <ellipse
            cx="100"
            cy="160"
            rx="32"
            ry="25"
            fill="url(#lightFur)"
          />
        </g>

        {/* 头部 */}
        <g className={styles.head}>
          {/* 左耳 - 三角形猫耳 */}
          <g className={styles.leftEar}>
            <path
              d="M52 85 L62 35 L85 70 Z"
              fill="url(#furGradient)"
              filter="url(#shadow)"
            />
            <path
              d="M58 78 L65 45 L78 70 Z"
              fill="url(#innerEar)"
            />
          </g>

          {/* 右耳 - 三角形猫耳 */}
          <g className={styles.rightEar}>
            <path
              d="M148 85 L138 35 L115 70 Z"
              fill="url(#furGradient)"
              filter="url(#shadow)"
            />
            <path
              d="M142 78 L135 45 L122 70 Z"
              fill="url(#innerEar)"
            />
          </g>

          {/* 头部主体 - 圆形 */}
          <ellipse
            cx="100"
            cy="100"
            rx="52"
            ry="48"
            fill="url(#furGradient)"
            filter="url(#shadow)"
          />

          {/* 头部条纹 */}
          <ellipse
            cx="100"
            cy="100"
            rx="52"
            ry="48"
            fill="url(#stripes)"
          />

          {/* 脸部浅色区域 */}
          <ellipse
            cx="100"
            cy="110"
            rx="38"
            ry="32"
            fill="url(#lightFur)"
          />

          {/* 额头花纹 */}
          <path
            d="M90 75 L100 65 L110 75"
            fill="none"
            stroke="#E88A3C"
            strokeWidth="2.5"
            strokeLinecap="round"
            opacity="0.6"
          />

          {/* 眼睛 */}
          <g className={styles.eyes}>
            {/* 左眼 */}
            <g className={styles.leftEye}>
              {/* 眼白 */}
              <ellipse cx="78" cy="95" rx="14" ry="16" fill="#FFFFFF" />
              {/* 瞳孔 - 猫眼竖瞳 */}
              <ellipse
                cx={78 + eyeOffset.x}
                cy={95 + eyeOffset.y}
                rx="6"
                ry="12"
                fill="#2D4A1C"
              />
              {/* 瞳孔中心 */}
              <ellipse
                cx={78 + eyeOffset.x}
                cy={95 + eyeOffset.y}
                rx="3"
                ry="10"
                fill="#1A2E0F"
              />
              {/* 高光 */}
              <circle
                cx={76 + eyeOffset.x * 0.3}
                cy={90 + eyeOffset.y * 0.3}
                r="3"
                fill="#FFFFFF"
              />
              <circle
                cx={81 + eyeOffset.x * 0.2}
                cy={98 + eyeOffset.y * 0.2}
                r="1.5"
                fill="#FFFFFF"
                opacity="0.7"
              />
            </g>

            {/* 右眼 */}
            <g className={styles.rightEye}>
              {/* 眼白 */}
              <ellipse cx="122" cy="95" rx="14" ry="16" fill="#FFFFFF" />
              {/* 瞳孔 - 猫眼竖瞳 */}
              <ellipse
                cx={122 + eyeOffset.x}
                cy={95 + eyeOffset.y}
                rx="6"
                ry="12"
                fill="#2D4A1C"
              />
              {/* 瞳孔中心 */}
              <ellipse
                cx={122 + eyeOffset.x}
                cy={95 + eyeOffset.y}
                rx="3"
                ry="10"
                fill="#1A2E0F"
              />
              {/* 高光 */}
              <circle
                cx={120 + eyeOffset.x * 0.3}
                cy={90 + eyeOffset.y * 0.3}
                r="3"
                fill="#FFFFFF"
              />
              <circle
                cx={125 + eyeOffset.x * 0.2}
                cy={98 + eyeOffset.y * 0.2}
                r="1.5"
                fill="#FFFFFF"
                opacity="0.7"
              />
            </g>
          </g>

          {/* 遮眼爪子 */}
          <g className={`${styles.peekPaws} ${isTyping ? styles.peekPawsActive : ''}`}>
            <g className={styles.peekLeftPaw}>
              <ellipse cx="65" cy="98" rx="20" ry="16" fill="url(#furGradient)" />
              <ellipse cx="62" cy="102" rx="4" ry="3" fill="#FFB5B5" />
              <ellipse cx="68" cy="102" rx="4" ry="3" fill="#FFB5B5" />
              <ellipse cx="65" cy="107" rx="5" ry="4" fill="#FFB5B5" />
            </g>
            <g className={styles.peekRightPaw}>
              <ellipse cx="135" cy="98" rx="20" ry="16" fill="url(#furGradient)" />
              <ellipse cx="132" cy="102" rx="4" ry="3" fill="#FFB5B5" />
              <ellipse cx="138" cy="102" rx="4" ry="3" fill="#FFB5B5" />
              <ellipse cx="135" cy="107" rx="5" ry="4" fill="#FFB5B5" />
            </g>
          </g>

          {/* 鼻子 - 倒三角形 */}
          <g className={styles.nose}>
            <path
              d="M100 115 L94 122 L106 122 Z"
              fill="url(#noseColor)"
            />
            <ellipse cx="100" cy="118" rx="2" ry="1.5" fill="#FFFFFF" opacity="0.5" />
          </g>

          {/* 嘴巴 */}
          <g className={styles.mouth}>
            <path
              d="M100 122 L100 127"
              stroke="#E88A6A"
              strokeWidth="1.5"
              strokeLinecap="round"
            />
            <path
              d="M88 130 Q100 140, 112 130"
              fill="none"
              stroke="#E88A6A"
              strokeWidth="1.5"
              strokeLinecap="round"
            />
          </g>

          {/* 胡须 */}
          <g className={styles.whiskers}>
            <line x1="45" y1="108" x2="72" y2="112" stroke="#D4A574" strokeWidth="1.2" strokeLinecap="round" />
            <line x1="42" y1="118" x2="70" y2="118" stroke="#D4A574" strokeWidth="1.2" strokeLinecap="round" />
            <line x1="45" y1="128" x2="72" y2="124" stroke="#D4A574" strokeWidth="1.2" strokeLinecap="round" />
            <line x1="155" y1="108" x2="128" y2="112" stroke="#D4A574" strokeWidth="1.2" strokeLinecap="round" />
            <line x1="158" y1="118" x2="130" y2="118" stroke="#D4A574" strokeWidth="1.2" strokeLinecap="round" />
            <line x1="155" y1="128" x2="128" y2="124" stroke="#D4A574" strokeWidth="1.2" strokeLinecap="round" />
          </g>
        </g>

        {/* 前爪 */}
        <g className={styles.frontPaws}>
          <ellipse cx="75" cy="188" rx="16" ry="10" fill="url(#furGradient)" />
          <ellipse cx="71" cy="190" rx="3" ry="2.5" fill="#FFB5B5" />
          <ellipse cx="78" cy="190" rx="3" ry="2.5" fill="#FFB5B5" />
          <ellipse cx="75" cy="194" rx="4" ry="3" fill="#FFB5B5" />

          <ellipse cx="125" cy="188" rx="16" ry="10" fill="url(#furGradient)" />
          <ellipse cx="121" cy="190" rx="3" ry="2.5" fill="#FFB5B5" />
          <ellipse cx="128" cy="190" rx="3" ry="2.5" fill="#FFB5B5" />
          <ellipse cx="125" cy="194" rx="4" ry="3" fill="#FFB5B5" />
        </g>
      </svg>

      {/* 表情气泡 */}
      <div className={`${styles.speechBubble} ${isTyping ? styles.speechBubbleVisible : ''}`}>
        <span>🙈</span>
        <span className={styles.speechText}>不看...</span>
      </div>
    </div>
  );
};

export default InteractiveCat;
