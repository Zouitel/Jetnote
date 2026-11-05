export interface RateLimitState {
  minuteWindow: number;
  minuteCount: number;
  dayWindow: number;
  dayCount: number;
}

export interface RateLimitDecision {
  allowed: boolean;
  state: RateLimitState;
  retryAfterSeconds: number;
}

export function evaluateRateLimit(
  current: Partial<RateLimitState> | undefined,
  nowMs: number,
  perMinute: number,
  perDay: number
): RateLimitDecision {
  const minuteWindow = Math.floor(nowMs / 60_000);
  const dayWindow = Math.floor(nowMs / 86_400_000);

  const state: RateLimitState = {
    minuteWindow,
    minuteCount: current?.minuteWindow === minuteWindow ? (current.minuteCount ?? 0) : 0,
    dayWindow,
    dayCount: current?.dayWindow === dayWindow ? (current.dayCount ?? 0) : 0
  };

  if (state.minuteCount >= perMinute) {
    return {
      allowed: false,
      state,
      retryAfterSeconds: Math.max(1, Math.ceil(((minuteWindow + 1) * 60_000 - nowMs) / 1_000))
    };
  }

  if (state.dayCount >= perDay) {
    return {
      allowed: false,
      state,
      retryAfterSeconds: Math.max(1, Math.ceil(((dayWindow + 1) * 86_400_000 - nowMs) / 1_000))
    };
  }

  return {
    allowed: true,
    state: {
      ...state,
      minuteCount: state.minuteCount + 1,
      dayCount: state.dayCount + 1
    },
    retryAfterSeconds: 0
  };
}
