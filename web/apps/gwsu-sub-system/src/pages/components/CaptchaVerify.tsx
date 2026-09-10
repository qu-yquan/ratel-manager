import React, {useCallback, useLayoutEffect, useRef, useState} from 'react';
import {App, Modal} from 'antd';
import CryptoJS from 'crypto-js';
import {
    CaptchaData,
    CaptchaGetResponse,
    CaptchaType,
    checkCaptcha,
    getCaptcha,
} from '../../services/login';
import styles from './CaptchaVerify.module.less';

interface Point {
    x: number;
    y: number;
}

export interface CaptchaPass {
    captchaId: string;
    captchaCode: string;
}

interface CaptchaVerifyProps {
    open: boolean;
    onCancel: () => void;
    onSuccess: (value: CaptchaPass) => void;
}

const BLOCK_PUZZLE_TYPE = 'blockPuzzle';
const BLOCK_PUZZLE_DEFAULT_Y = 5;

function imageSource(base64?: string) {
    return base64 ? `data:image/png;base64,${base64}` : '';
}

function encryptText(plainText: string, secretKey?: string) {
    if (!secretKey) {
        return plainText;
    }
    return CryptoJS.AES.encrypt(plainText, CryptoJS.enc.Utf8.parse(secretKey), {
        mode: CryptoJS.mode.ECB,
        padding: CryptoJS.pad.Pkcs7,
    }).toString();
}

function captchaToken(data: CaptchaData) {
    if (data.token) {
        return data.token;
    }
    return data.captchaId.substring(data.captchaId.indexOf(':') + 1);
}

function buildCaptchaCode(data: CaptchaData, plainPointJson: string) {
    return encryptText(`${captchaToken(data)}---${plainPointJson}`, data.secretKey);
}

const CaptchaVerify: React.FC<CaptchaVerifyProps> = ({open, onCancel, onSuccess}) => {
    const {message} = App.useApp();
    const [captcha, setCaptcha] = useState<CaptchaGetResponse | null>(null);
    const [loading, setLoading] = useState(false);
    const [checking, setChecking] = useState(false);
    const [sliderX, setSliderX] = useState(0);
    const [dragging, setDragging] = useState(false);
    const [clickPoints, setClickPoints] = useState<Point[]>([]);
    const dragStartRef = useRef({pointerX: 0, sliderX: 0});
    const sliderXRef = useRef(0);
    const trackRef = useRef<HTMLDivElement | null>(null);
    const captchaImageRef = useRef<HTMLImageElement | null>(null);
    const jigsawImageRef = useRef<HTMLImageElement | null>(null);
    const openRef = useRef(false);
    const sessionSequenceRef = useRef(0);
    const requestSequenceRef = useRef(0);

    const resetCaptchaState = useCallback(() => {
        setCaptcha(null);
        setLoading(false);
        setChecking(false);
        setSliderX(0);
        sliderXRef.current = 0;
        setDragging(false);
        setClickPoints([]);
    }, []);

    const isCurrentRequest = useCallback((sessionSequence: number, requestSequence: number) => (
        openRef.current
        && sessionSequence === sessionSequenceRef.current
        && requestSequence === requestSequenceRef.current
    ), []);

    const refreshCaptcha = useCallback(async (sessionSequence = sessionSequenceRef.current) => {
        if (!openRef.current || sessionSequence !== sessionSequenceRef.current) {
            return;
        }

        const requestSequence = ++requestSequenceRef.current;
        setCaptcha(null);
        setLoading(true);
        setChecking(false);
        setSliderX(0);
        sliderXRef.current = 0;
        setDragging(false);
        setClickPoints([]);

        try {
            const data = await getCaptcha();
            if (isCurrentRequest(sessionSequence, requestSequence)) {
                setCaptcha(data);
            }
        } catch {
            if (isCurrentRequest(sessionSequence, requestSequence)) {
                setCaptcha(null);
                message.error('验证码加载失败，请稍后重试');
            }
        } finally {
            if (isCurrentRequest(sessionSequence, requestSequence)) {
                setLoading(false);
            }
        }
    }, [isCurrentRequest, message]);

    useLayoutEffect(() => {
        const sessionSequence = ++sessionSequenceRef.current;
        requestSequenceRef.current += 1;
        openRef.current = open;

        if (open) {
            void refreshCaptcha(sessionSequence);
        } else {
            resetCaptchaState();
        }

        return () => {
            if (sessionSequence === sessionSequenceRef.current) {
                openRef.current = false;
                sessionSequenceRef.current += 1;
                requestSequenceRef.current += 1;
            }
        };
    }, [open, refreshCaptcha, resetCaptchaState]);

    const captchaData: CaptchaData | null = captcha?.data ?? null;
    const isBlockPuzzle = captchaData?.captchaType === BLOCK_PUZZLE_TYPE || captcha?.type === CaptchaType.BLOCK_PUZZLE;
    const requiredClickCount = captchaData?.wordList?.length ?? 0;

    const submitCaptchaCheck = useCallback(async (plainPointJson: string) => {
        if (!openRef.current) {
            return;
        }
        if (!captchaData?.captchaId) {
            message.warning('请先获取验证码');
            return;
        }

        const pointJson = encryptText(plainPointJson, captchaData.secretKey);
        const captchaCode = buildCaptchaCode(captchaData, plainPointJson);
        const sessionSequence = sessionSequenceRef.current;
        const requestSequence = ++requestSequenceRef.current;
        setChecking(true);
        try {
            const result = await checkCaptcha({
                captchaId: captchaData.captchaId,
                captchaCode,
                pointJson,
            });
            if (!isCurrentRequest(sessionSequence, requestSequence)) {
                return;
            }
            onSuccess({
                captchaId: result.captchaId,
                captchaCode: result.captchaCode,
            });
            message.success('验证码校验通过');
        } catch {
            if (isCurrentRequest(sessionSequence, requestSequence)) {
                await refreshCaptcha(sessionSequence);
            }
        } finally {
            if (isCurrentRequest(sessionSequence, requestSequence)) {
                setChecking(false);
                setDragging(false);
            }
        }
    }, [captchaData, isCurrentRequest, message, onSuccess, refreshCaptcha]);

    const handleSliderPointerDown = (event: React.PointerEvent<HTMLButtonElement>) => {
        if (!captchaData || checking) {
            return;
        }
        event.currentTarget.setPointerCapture(event.pointerId);
        dragStartRef.current = {
            pointerX: event.clientX,
            sliderX,
        };
        setDragging(true);
    };

    const handleSliderPointerMove = (event: React.PointerEvent<HTMLButtonElement>) => {
        if (!dragging || !trackRef.current) {
            return;
        }

        const captchaImageWidth = captchaImageRef.current?.clientWidth ?? trackRef.current.clientWidth;
        const jigsawImageWidth = jigsawImageRef.current?.clientWidth ?? 42;
        const max = Math.max(captchaImageWidth - jigsawImageWidth, 0);
        const nextX = Math.min(Math.max(dragStartRef.current.sliderX + event.clientX - dragStartRef.current.pointerX, 0), max);
        sliderXRef.current = nextX;
        setSliderX(nextX);
    };

    const handleSliderPointerUp = async (event: React.PointerEvent<HTMLButtonElement>) => {
        if (!dragging || !captchaImageRef.current || !captchaData) {
            setDragging(false);
            return;
        }
        event.currentTarget.releasePointerCapture(event.pointerId);

        const image = captchaImageRef.current;
        const scale = image.naturalWidth / image.clientWidth;
        const pointJson = JSON.stringify({
            x: Math.round(sliderXRef.current * scale),
            y: BLOCK_PUZZLE_DEFAULT_Y,
        });
        await submitCaptchaCheck(pointJson);
    };

    const handleClickWord = async (event: React.MouseEvent<HTMLImageElement>) => {
        if (!captchaData || checking || requiredClickCount <= 0) {
            return;
        }

        const rect = event.currentTarget.getBoundingClientRect();
        const scale = event.currentTarget.naturalWidth / rect.width;
        const point = {
            x: Math.round((event.clientX - rect.left) * scale),
            y: Math.round((event.clientY - rect.top) * scale),
        };
        const points = [...clickPoints, point];
        setClickPoints(points);

        if (points.length >= requiredClickCount) {
            const pointJson = JSON.stringify(points);
            await submitCaptchaCheck(pointJson);
        }
    };

    const handleCancel = () => {
        if (!checking) {
            onCancel();
        }
    };

    return (
        <Modal
            title="安全验证"
            open={open}
            onCancel={handleCancel}
            footer={null}
            centered
            width={420}
            maskClosable={!checking}
            keyboard={!checking}
            closable={!checking}
        >
            <div className={styles.captchaPanel} aria-busy={loading || checking}>
                <div className={styles.captchaPanelHeader}>
                    <span>{isBlockPuzzle ? '拖动滑块完成拼图' : '请依次点击文字'}</span>
                    <button
                        type="button"
                        className={styles.captchaRefresh}
                        onClick={() => void refreshCaptcha()}
                        disabled={loading || checking}
                        aria-label="更换验证码"
                    >
                        换一张
                    </button>
                </div>

                {loading || !captchaData ? (
                    <div className={styles.captchaLoading}>验证码加载中...</div>
                ) : isBlockPuzzle ? (
                    <>
                        <div className={styles.captchaImageBox}>
                            <img
                                ref={captchaImageRef}
                                src={imageSource(captchaData.originalImageBase64)}
                                className={styles.captchaImage}
                                alt="滑块验证码背景"
                                draggable={false}
                            />
                            <img
                                ref={jigsawImageRef}
                                src={imageSource(captchaData.jigsawImageBase64)}
                                className={styles.jigsawImage}
                                alt="滑块拼图"
                                draggable={false}
                                style={{transform: `translateX(${sliderX}px)`}}
                            />
                        </div>
                        <div ref={trackRef} className={styles.sliderTrack}>
                            <div className={styles.sliderProgress} style={{width: `${sliderX + 42}px`}}/>
                            <button
                                type="button"
                                className={styles.sliderHandle}
                                style={{transform: `translateX(${sliderX}px)`}}
                                onPointerDown={handleSliderPointerDown}
                                onPointerMove={handleSliderPointerMove}
                                onPointerUp={handleSliderPointerUp}
                                disabled={checking}
                                aria-label="拖动滑块完成安全验证"
                            >
                                {checking ? '...' : '→'}
                            </button>
                            <span className={styles.sliderText}>向右拖动滑块</span>
                        </div>
                    </>
                ) : (
                    <div className={styles.clickWordBox}>
                        <div className={styles.wordTip}>
                            请依次点击：{captchaData.wordList?.join('、') || '图中文字'}
                        </div>
                        <div className={styles.clickImageWrap}>
                            <img
                                ref={captchaImageRef}
                                src={imageSource(captchaData.originalImageBase64)}
                                className={styles.captchaImage}
                                alt="文字点选验证码"
                                draggable={false}
                                onClick={handleClickWord}
                            />
                            {clickPoints.map((point, index) => (
                                <span
                                    key={`${point.x}-${point.y}-${index}`}
                                    className={styles.clickMarker}
                                    style={{
                                        left: `${point.x / (captchaImageRef.current?.naturalWidth || 1) * 100}%`,
                                        top: `${point.y / (captchaImageRef.current?.naturalHeight || 1) * 100}%`,
                                    }}
                                >
                                    {index + 1}
                                </span>
                            ))}
                        </div>
                    </div>
                )}
            </div>
        </Modal>
    );
};

export default CaptchaVerify;
