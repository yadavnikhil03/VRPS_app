import datetime as dt
import os
import subprocess
from collections import Counter


def get_commit_days(days_back: int = 90):
    since = (dt.datetime.now(dt.UTC) - dt.timedelta(days=days_back)).strftime("%Y-%m-%d")
    cmd = [
        "git",
        "log",
        f"--since={since}",
        "--date=short",
        "--pretty=format:%ad",
    ]
    out = subprocess.check_output(cmd, text=True, encoding="utf-8", errors="ignore")
    dates = [line.strip() for line in out.splitlines() if line.strip()]
    return Counter(dates)


def get_recent_commit_dates(limit: int = 60):
    cmd = [
        "git",
        "log",
        f"-n{limit}",
        "--date=short",
        "--pretty=format:%ad",
    ]
    out = subprocess.check_output(cmd, text=True, encoding="utf-8", errors="ignore")
    dates = [line.strip() for line in out.splitlines() if line.strip()]
    dates.reverse()  # Oldest -> newest for left-to-right timeline
    return dates


def build_svg(counts: Counter, recent_commits, days_back: int = 90) -> str:
    end = dt.date.today()
    start = end - dt.timedelta(days=days_back - 1)
    days = [start + dt.timedelta(days=i) for i in range(days_back)]
    values = [counts.get(d.isoformat(), 0) for d in days]
    max_val = max(values) if values else 1

    width = 980
    height = 300
    pad_left = 46
    pad_right = 20
    pad_top = 26
    pad_bottom = 70
    chart_w = width - pad_left - pad_right
    chart_h = height - pad_top - pad_bottom

    bar_gap = 2
    bar_w = max(1, int((chart_w - (days_back - 1) * bar_gap) / days_back))

    title = "Repo Commit Activity (Last 90 Days)"
    subtitle = f"Total commits: {sum(values)} | Active days: {sum(1 for v in values if v > 0)}"

    bg = "#0f172a"
    grid = "#334155"
    text = "#cbd5e1"
    bar = "#14b8a6"
    bar_low = "#22d3ee"

    parts = []
    parts.append(f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" viewBox="0 0 {width} {height}">')
    parts.append(f'<rect width="100%" height="100%" fill="{bg}" rx="12"/>')
    parts.append(f'<text x="{pad_left}" y="18" fill="{text}" font-family="Segoe UI, Arial, sans-serif" font-size="14" font-weight="600">{title}</text>')
    parts.append(f'<text x="{pad_left}" y="36" fill="{text}" opacity="0.75" font-family="Segoe UI, Arial, sans-serif" font-size="11">{subtitle}</text>')

    # Grid lines
    for i in range(5):
        y = pad_top + int(chart_h * i / 4)
        parts.append(f'<line x1="{pad_left}" y1="{y}" x2="{width - pad_right}" y2="{y}" stroke="{grid}" stroke-width="1" opacity="0.45"/>')

    # Bars
    x = pad_left
    for v in values:
        h = int((v / max_val) * (chart_h - 2)) if max_val > 0 else 0
        y = pad_top + chart_h - h
        color = bar if v > (max_val * 0.35) else bar_low
        if h > 0:
            parts.append(f'<rect x="{x}" y="{y}" width="{bar_w}" height="{h}" fill="{color}" rx="1"/>')
        x += bar_w + bar_gap

    # Axis labels (month markers)
    month_marks = {}
    for i, d in enumerate(days):
        if d.day == 1 or i == 0:
            month_marks[i] = d.strftime("%b")

    for idx, label in month_marks.items():
        lx = pad_left + idx * (bar_w + bar_gap)
        ly = height - 44
        parts.append(f'<text x="{lx}" y="{ly}" fill="{text}" opacity="0.7" font-family="Segoe UI, Arial, sans-serif" font-size="10">{label}</text>')

    # Recent commit timeline (shows each commit, including same-day commits)
    timeline_y = height - 24
    parts.append(f'<text x="{pad_left}" y="{timeline_y - 8}" fill="{text}" opacity="0.8" font-family="Segoe UI, Arial, sans-serif" font-size="10">Recent commits</text>')
    parts.append(f'<line x1="{pad_left}" y1="{timeline_y}" x2="{width - pad_right}" y2="{timeline_y}" stroke="{grid}" stroke-width="1" opacity="0.5"/>')

    commit_count = len(recent_commits)
    if commit_count > 0:
        step = chart_w / max(1, commit_count - 1)
        for i, commit_date in enumerate(recent_commits):
            cx = int(pad_left + i * step)
            # highlight today's commits brighter
            is_today = commit_date == end.isoformat()
            fill = "#2dd4bf" if is_today else "#67e8f9"
            parts.append(f'<circle cx="{cx}" cy="{timeline_y}" r="3" fill="{fill}"/>')

    parts.append('</svg>')
    return "\n".join(parts)


def main():
    counts = get_commit_days(90)
    recent_commits = get_recent_commit_dates(80)
    svg = build_svg(counts, recent_commits, 90)
    out_path = os.path.join("docs", "repo-activity.svg")
    os.makedirs(os.path.dirname(out_path), exist_ok=True)
    with open(out_path, "w", encoding="utf-8") as f:
        f.write(svg)


if __name__ == "__main__":
    main()
