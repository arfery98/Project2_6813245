"""Black-box checks for daily ordering, accounting and configuration handling."""
from pathlib import Path
import re
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
PACKAGE = "Project2_6813245"


def check_run(output, days, brokers, assessors, gateways, tokens, batch, low, high):
    pending = {i: 0 for i in range(gateways)}
    paid_by = {f"AssessorThread_{i}": 0 for i in range(assessors)}
    carried = dict.fromkeys(paid_by, 0)
    remaining_tokens = {}
    arrivals = {}
    submissions = {}
    seen_assessors = set()
    pending_seen = set()
    day = 0
    assessor_phase = False
    summary = []

    def finish_day():
        if day:
            assert len(arrivals) == brokers
            assert all(submissions.get(name, 0) == count for name, count in arrivals.items())
            assert len(seen_assessors) == assessors
            assert len(pending_seen) == assessors
            assert len(remaining_tokens) == 2

    for line in output.splitlines():
        match = re.fullmatch(r"(\S+) >> (.*)", line)
        assert match, line
        name, text = match.groups()
        m = re.fullmatch(r"Day (\d+)", text)
        if m:
            finish_day()
            day += 1
            assert int(m[1]) == day
            arrivals, submissions, remaining_tokens = {}, {}, {}
            seen_assessors, pending_seen = set(), set()
            assessor_phase = False
        elif m := re.fullmatch(r"ClaimGateway_(\d+) pending claims = (\d+)", text):
            assert int(m[2]) == pending[int(m[1])]
        elif m := re.fullmatch(r"reset PaymentGateway_(\d+) tokens = (\d+)", text):
            assert not arrivals
            assert int(m[2]) == tokens
            remaining_tokens[int(m[1])] = tokens
        elif m := re.fullmatch(r"Day (\d+) new claims = (\d+); split to \[(.*)\]", text):
            assert not assessor_phase
            assert len(remaining_tokens) == 2
            assert int(m[1]) == day
            count = int(m[2])
            assert low <= count <= high
            batches = [int(x) for x in m[3].split(", ")] if m[3] else []
            assert sum(batches) == count
            assert all(0 < x <= batch for x in batches)
            assert name not in arrivals
            arrivals[name] = count
        elif m := re.fullmatch(r"submit (\d+) claims to ClaimGateway_(\d+); pending claims = (\d+)", text):
            assert not assessor_phase
            amount, gateway, after = map(int, m.groups())
            assert 0 < amount <= batch
            pending[gateway] += amount
            assert pending[gateway] == after
            submissions[name] = submissions.get(name, 0) + amount
        elif m := re.fullmatch(r"approve (\d+) claims from ClaimGateway_(\d+); pending claims = (\d+)", text):
            assert len(arrivals) == brokers
            assert all(submissions.get(n, 0) == c for n, c in arrivals.items())
            assessor_phase = True
            assert name not in seen_assessors
            seen_assessors.add(name)
            amount, gateway, after = map(int, m.groups())
            pending[gateway] -= amount
            carried[name] += amount
            assert pending[gateway] == after >= 0
        elif m := re.fullmatch(r"pending payments = (\d+)", text):
            assert int(m[1]) == carried[name]
            assert name not in pending_seen
            pending_seen.add(name)
        elif m := re.fullmatch(r"issue (\d+) payments to PaymentGateway_(\d+); tokens = (\d+)", text):
            amount, gateway, after = map(int, m.groups())
            assert name in pending_seen
            remaining_tokens[gateway] -= amount
            carried[name] -= amount
            paid_by[name] += amount
            assert remaining_tokens[gateway] == after >= 0
            assert carried[name] >= 0
        elif m := re.fullmatch(r"(AssessorThread_\d+) successful payments = (\d+)", text):
            assert name == "main"
            summary.append((m[1], int(m[2])))
    finish_day()
    assert day == days
    assert summary == sorted(paid_by.items(), key=lambda pair: (-pair[1], pair[0]))


with tempfile.TemporaryDirectory(prefix="part5-check-") as tmp:
    root = Path(tmp)
    source = root / "src/main/java" / PACKAGE
    source.parent.mkdir(parents=True)
    shutil.copytree(ROOT / "src/main/java" / PACKAGE, source)
    subprocess.run(["javac", "-d", str(root / "classes"),
                    *map(str, source.glob("*.java"))], check=True, capture_output=True, text=True)
    command = ["java", "-cp", str(root / "classes"), f"{PACKAGE}.Main"]
    cases = [(6, 3, 3, 3, 80, 50, 20, 100)] * 8
    cases += [(4, 4, 12, 1, 0, 3, 7, 7),  # token exhaustion, ties, partial batch
              (4, 3, 4, 1, 2, 3, 7, 7),   # gateway contention and carry-over
              (3, 2, 3, 2, 5, 3, 0, 0),   # no new claims
              (3, 0, 3, 2, 5, 3, 0, 0),   # no brokers
              (3, 2, 0, 2, 5, 3, 7, 7),   # no assessors
              (0, 2, 3, 2, 5, 3, 7, 7)]   # zero days
    for args in cases:
        days, brokers, assessors, gateways, tokens, batch, low, high = args
        (source / "test.txt").write_text(
            f"# test configuration\ndays,{days}\nClaimGateways_num_batch,{gateways},{batch}\n"
            f"PaymentGateways_num_max,2,{tokens}\nBrokerThreads_num_min_max,{brokers},{low},{high}\n"
            f"AssessorThreads_num_max,{assessors},80\n", encoding="utf-8")
        run = subprocess.run(command + ["test.txt"], cwd=root, capture_output=True,
                             text=True, timeout=15)
        assert run.returncode == 0, run.stderr
        check_run(run.stdout, *args)

    # Retry more than once, then recover using a valid package-relative filename.
    run = subprocess.run(command, cwd=root, input="missing.txt\ntest.txt\n",
                         capture_output=True, text=True, timeout=15)
    assert run.returncode == 0, run.stderr
    assert run.stdout.count("New file name = ") == 2
    assert "Summary" in run.stdout
    run = subprocess.run(command, cwd=root, input="", capture_output=True, text=True, timeout=15)
    assert run.returncode != 0 and "No configuration file supplied" in run.stderr
    assert "Exception in thread" not in run.stderr

print(f"PASS: compilation, {len(cases)} simulation runs, missing-file retry and EOF handling")
