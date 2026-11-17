# VORION AI

VORION AI is an independent intelligent-assistant project with its own name, product identity, interaction layer, and extensible intelligence gateway.

## Current milestone — 0.1.0 foundation

The repository contains:
- An Android-native Kotlin and Jetpack Compose foundation.
- A reusable animated holographic presence component with explicit assistant states.
- A provider-neutral FastAPI gateway for external AI inference.
- An Android HTTP client that talks only to the VORION gateway.
- A GitHub Actions workflow that attempts a debug Android build.

## Architecture

1. **VORION Android app** — user interface, future voice pipeline, and permission-aware device integrations.
2. **VORION gateway** — validates app access, applies the VORION system identity, and forwards requests to a configured compatible AI provider.
3. **External model provider** — replaceable inference engine. Its identity and credentials are not part of the Android UI or APK.
4. **Future VORION intelligence layer** — orchestration, memory, tool execution, evaluation, and eventually specialized models.

## Backend configuration

Run from the backend directory:

~~~bash
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
~~~

Set these environment variables on the backend host (never in the Android app):

- VORION_APP_TOKEN — a long, random application access token.
- AI_PROVIDER_API_KEY — secret key for the selected AI provider.
- AI_PROVIDER_MODEL — model identifier supported by the selected provider (for OpenRouter, use its `provider/model` slug).
- AI_PROVIDER_BASE_URL — optional OpenAI-compatible API base URL; defaults to `https://openrouter.ai/api/v1`.

### OpenRouter setup

For OpenRouter, configure the backend host with:

~~~env
AI_PROVIDER_BASE_URL=https://openrouter.ai/api/v1
AI_PROVIDER_API_KEY=<your OpenRouter secret key>
AI_PROVIDER_MODEL=<provider/model slug selected from the OpenRouter model catalog>
~~~

Keep `AI_PROVIDER_API_KEY` only in the backend host's secret/environment-variable settings. Never commit it to this repository or include it in the Android app. OpenRouter uses the standard Bearer authorization header and the OpenAI-compatible Chat Completions endpoint.

Start locally with:

~~~bash
uvicorn app.main:app --host 0.0.0.0 --port 8000
~~~

Health check: GET /health

Chat endpoint: POST /v1/chat/completions with a Bearer app token and a JSON body containing a messages array. Only user and assistant roles are accepted from clients; the gateway inserts the VORION system identity itself.

## Android gateway client

The ai.vorion.network.VorionApiClient class accepts a gateway base URL and a session-token provider. The app must obtain its access token through a real authentication/session flow. Do not hardcode the backend token or provider API key in source code, Gradle files, or the APK.

## Security and release status

- Use HTTPS for any deployed gateway.
- Keep provider credentials exclusively in backend environment variables/secrets.
- The current shared app token is a bootstrap mechanism, not a production user-authentication system. Before public release, add per-user authentication, rate limiting, quotas, abuse monitoring, and secret rotation.
- Sensitive device actions must require explicit confirmation and Android permissions.
- No wake-word detection, background listening, device-wide control, production backend deployment, or production authentication has been completed.
- A workflow has been added to verify Android compilation. Until it completes successfully and the APK is tested on a device, this is not a release-ready build.

## Hologram states

IDLE, LISTENING, THINKING, SPEAKING, EXECUTING, SUCCESS, WARNING, and ERROR.
