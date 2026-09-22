import type { FieldError, ProblemDetails } from './types'

export class ApiError extends Error {
  readonly status: number
  readonly problem: ProblemDetails
  readonly fieldErrors: Record<string, string>

  constructor(problem: ProblemDetails) {
    super(problem.detail ?? problem.title ?? 'Request failed')
    this.name = 'ApiError'
    this.status = problem.status
    this.problem = problem
    this.fieldErrors = toFieldErrorMap(problem.errors)
  }
}

function toFieldErrorMap(errors: FieldError[] | undefined): Record<string, string> {
  if (!errors) {
    return {}
  }
  return errors.reduce<Record<string, string>>((acc, item) => {
    acc[item.field] = item.message
    return acc
  }, {})
}

export function userMessageForError(error: unknown): string {
  if (error instanceof TypeError) {
    return 'Unable to connect to the ticket service. Please try again.'
  }
  if (error instanceof ApiError) {
    if (error.status === 400) {
      return error.problem.detail?.trim()
        ? error.problem.detail
        : 'Please check the highlighted fields.'
    }
    if (error.status === 404) {
      return 'Ticket not found.'
    }
    if (error.status === 409) {
      const detail = error.problem.detail?.trim()
      if (detail) {
        return detail
      }
      return 'This status change is not allowed.'
    }
    return error.message || 'Something went wrong. Please try again.'
  }
  if (error instanceof Error && error.message) {
    return error.message
  }
  return 'Something went wrong. Please try again.'
}

async function parseProblem(response: Response): Promise<ProblemDetails> {
  try {
    const body = (await response.json()) as ProblemDetails
    return {
      ...body,
      status: body.status ?? response.status,
    }
  } catch {
    return {
      status: response.status,
      title: response.statusText || 'Error',
      detail: 'Something went wrong. Please try again.',
    }
  }
}

export async function apiRequest<T>(
  path: string,
  init?: RequestInit,
): Promise<T> {
  let response: Response
  try {
    response = await fetch(path, {
      ...init,
      headers: {
        Accept: 'application/json',
        ...(init?.body ? { 'Content-Type': 'application/json' } : {}),
        ...init?.headers,
      },
    })
  } catch (error) {
    throw error instanceof TypeError
      ? error
      : new TypeError('Unable to connect to the ticket service. Please try again.')
  }

  if (!response.ok) {
    throw new ApiError(await parseProblem(response))
  }

  if (response.status === 204) {
    return undefined as T
  }

  return (await response.json()) as T
}
