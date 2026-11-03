import { isConstName, isFunctionName, isVarName, type VarName } from './ast';

export type TokenType = 'number' | 'variable' | 'constant' | 'function' | 'operator' | 'lparen' | 'rparen' | 'comma';

export interface Token {
  type: TokenType;
  text: string;
  /** Index of the first character in the source string, for error messages. */
  pos: number;
  value?: number;
}

/** A parse error that knows where in the formula it happened. */
export class ParseError extends Error {
  constructor(
    message: string,
    public readonly pos: number,
    public readonly length = 1,
  ) {
    super(message);
    this.name = 'ParseError';
  }
}

const OPERATORS = new Set(['+', '-', '*', '/', '^', '%']);

/**
 * Splits a formula into tokens and inserts implicit multiplication,
 * so `2x`, `3sin(x)` and `(x+1)(x-1)` work like they do on paper.
 *
 * @param allowedVars variables valid in the current mode (x, y, t or u, v, t)
 */
export function tokenize(src: string, allowedVars: readonly VarName[]): Token[] {
  const raw: Token[] = [];
  let i = 0;

  while (i < src.length) {
    const c = src[i];

    if (/\s/.test(c)) {
      i++;
      continue;
    }

    // Numbers: 12, 0.5, .5, 1e-3
    if (/[0-9.]/.test(c)) {
      const start = i;
      while (i < src.length && /[0-9.]/.test(src[i])) i++;
      if (i < src.length && /[eE]/.test(src[i]) && /[0-9+-]/.test(src[i + 1] ?? '')) {
        const save = i;
        i++;
        if (src[i] === '+' || src[i] === '-') i++;
        if (/[0-9]/.test(src[i] ?? '')) {
          while (i < src.length && /[0-9]/.test(src[i])) i++;
        } else {
          i = save; // "2e" followed by something else: treat e as the constant
        }
      }
      const text = src.slice(start, i);
      const value = Number(text);
      if (!Number.isFinite(value) || text === '.') {
        throw new ParseError(`"${text}" is not a valid number`, start, text.length);
      }
      raw.push({ type: 'number', text, pos: start, value });
      continue;
    }

    // Names: variables, constants, functions
    if (/[a-zA-Z_]/.test(c)) {
      const start = i;
      while (i < src.length && /[a-zA-Z0-9_]/.test(src[i])) i++;
      const name = src.slice(start, i).toLowerCase();
      if (isFunctionName(name)) {
        raw.push({ type: 'function', text: name, pos: start });
      } else if (isConstName(name)) {
        raw.push({ type: 'constant', text: name, pos: start });
      } else if (isVarName(name)) {
        if (!allowedVars.includes(name)) {
          throw new ParseError(
            `"${name}" can't be used here. Available variables: ${allowedVars.join(', ')}`,
            start,
            name.length,
          );
        }
        raw.push({ type: 'variable', text: name, pos: start });
      } else {
        throw new ParseError(unknownNameMessage(name, allowedVars), start, name.length);
      }
      continue;
    }

    if (OPERATORS.has(c)) {
      raw.push({ type: 'operator', text: c, pos: i });
    } else if (c === '(') {
      raw.push({ type: 'lparen', text: c, pos: i });
    } else if (c === ')') {
      raw.push({ type: 'rparen', text: c, pos: i });
    } else if (c === ',') {
      raw.push({ type: 'comma', text: c, pos: i });
    } else {
      throw new ParseError(`Unexpected character "${c}"`, i);
    }
    i++;
  }

  return insertImplicitMultiplication(raw);
}

function insertImplicitMultiplication(tokens: Token[]): Token[] {
  const out: Token[] = [];
  for (let k = 0; k < tokens.length; k++) {
    const cur = tokens[k];
    const prev = out[out.length - 1];
    if (prev) {
      const prevEndsValue = prev.type === 'number' || prev.type === 'variable' || prev.type === 'constant' || prev.type === 'rparen';
      const curStartsValue = cur.type === 'variable' || cur.type === 'constant' || cur.type === 'function' || cur.type === 'lparen';
      if (prevEndsValue && curStartsValue) {
        out.push({ type: 'operator', text: '*', pos: cur.pos });
      } else if (prevEndsValue && cur.type === 'number') {
        throw new ParseError('Missing operator between two values', cur.pos, cur.text.length);
      }
    }
    if (prev?.type === 'function' && cur.type !== 'lparen') {
      throw new ParseError(`Write "${prev.text}(...)" with parentheses`, prev.pos, prev.text.length);
    }
    out.push(cur);
  }
  const last = out[out.length - 1];
  if (last?.type === 'function') {
    throw new ParseError(`Write "${last.text}(...)" with parentheses`, last.pos, last.text.length);
  }
  return out;
}

function unknownNameMessage(name: string, allowed: readonly VarName[]): string {
  // "xy" or "xyt" is almost always a forgotten multiplication sign.
  if (name.length > 1 && [...name].every((ch) => (allowed as readonly string[]).includes(ch))) {
    return `Unknown name "${name}". Did you mean ${[...name].join('*')}?`;
  }
  return `Unknown name "${name}"`;
}
