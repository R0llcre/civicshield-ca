#!/usr/bin/env python3
"""Run Gradle with idle/max-duration watchdog and persistent logs."""

from __future__ import annotations

import argparse
import os
import queue
import signal
import subprocess
import sys
import threading
import time
from collections import deque
from pathlib import Path


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Run a command with output-idle and total-duration watchdog."
    )
    parser.add_argument(
        "--idle-timeout-sec",
        type=int,
        default=120,
        help="Fail if no stdout/stderr output for this many seconds.",
    )
    parser.add_argument(
        "--max-duration-sec",
        type=int,
        default=1800,
        help="Fail if total runtime exceeds this many seconds.",
    )
    parser.add_argument(
        "--log-file",
        type=Path,
        required=True,
        help="Path to persist full process output.",
    )
    parser.add_argument(
        "--tail-lines",
        type=int,
        default=80,
        help="How many recent lines to print when failing.",
    )
    parser.add_argument("cmd", nargs=argparse.REMAINDER, help="Command to run.")
    args = parser.parse_args()
    if not args.cmd:
        parser.error("Missing command. Usage: ... -- ./gradlew ...")
    if args.cmd[0] == "--":
        args.cmd = args.cmd[1:]
    if not args.cmd:
        parser.error("Missing command after '--'.")
    return args


def reader_thread(stream, out_queue: queue.Queue[str | None]) -> None:
    try:
        for line in iter(stream.readline, ""):
            out_queue.put(line)
    finally:
        out_queue.put(None)


def terminate_process(proc: subprocess.Popen[str], grace_sec: float = 5.0) -> None:
    if proc.poll() is not None:
        return
    try:
        if os.name == "nt":
            proc.terminate()
        else:
            os.killpg(proc.pid, signal.SIGTERM)
    except ProcessLookupError:
        return
    except Exception:
        proc.terminate()
    deadline = time.time() + grace_sec
    while time.time() < deadline:
        if proc.poll() is not None:
            return
        time.sleep(0.1)
    try:
        if os.name == "nt":
            proc.kill()
        else:
            os.killpg(proc.pid, signal.SIGKILL)
    except ProcessLookupError:
        pass
    except Exception:
        proc.kill()


def main() -> int:
    args = parse_args()
    args.log_file.parent.mkdir(parents=True, exist_ok=True)
    tail: deque[str] = deque(maxlen=max(args.tail_lines, 10))
    start = time.time()
    last_output = start

    popen_kwargs: dict = {
        "stdout": subprocess.PIPE,
        "stderr": subprocess.STDOUT,
        "text": True,
        "bufsize": 1,
    }
    if os.name != "nt":
        popen_kwargs["preexec_fn"] = os.setsid

    with args.log_file.open("w", encoding="utf-8") as log_file:
        log_file.write(f"$ {' '.join(args.cmd)}\n")
        log_file.flush()
        print(f"[watchdog] log file: {args.log_file}")
        print(f"[watchdog] command: {' '.join(args.cmd)}")

        proc = subprocess.Popen(args.cmd, **popen_kwargs)
        assert proc.stdout is not None
        out_queue: queue.Queue[str | None] = queue.Queue()
        thread = threading.Thread(target=reader_thread, args=(proc.stdout, out_queue), daemon=True)
        thread.start()

        eof_seen = False
        timeout_reason: str | None = None

        while True:
            now = time.time()
            runtime = now - start
            idle = now - last_output
            if args.max_duration_sec > 0 and runtime > args.max_duration_sec:
                timeout_reason = (
                    f"max duration exceeded ({runtime:.1f}s > {args.max_duration_sec}s)"
                )
                terminate_process(proc)
                break
            if args.idle_timeout_sec > 0 and idle > args.idle_timeout_sec:
                timeout_reason = (
                    f"idle timeout exceeded ({idle:.1f}s > {args.idle_timeout_sec}s)"
                )
                terminate_process(proc)
                break

            try:
                item = out_queue.get(timeout=0.25)
            except queue.Empty:
                if eof_seen and proc.poll() is not None:
                    break
                continue

            if item is None:
                eof_seen = True
                if proc.poll() is not None:
                    break
                continue

            last_output = time.time()
            sys.stdout.write(item)
            log_file.write(item)
            tail.append(item.rstrip("\n"))

        thread.join(timeout=1.0)
        return_code = proc.poll()
        if return_code is None:
            terminate_process(proc)
            return_code = proc.wait(timeout=5)

        if timeout_reason is not None:
            print(f"[watchdog] terminated: {timeout_reason}", file=sys.stderr)
            log_file.write(f"\n[watchdog] terminated: {timeout_reason}\n")
            if tail:
                print("[watchdog] tail output:", file=sys.stderr)
                for line in tail:
                    print(line, file=sys.stderr)
            return 124

        if return_code != 0:
            print(f"[watchdog] command failed: exit {return_code}", file=sys.stderr)
            if tail:
                print("[watchdog] tail output:", file=sys.stderr)
                for line in tail:
                    print(line, file=sys.stderr)
        return int(return_code)


if __name__ == "__main__":
    raise SystemExit(main())
