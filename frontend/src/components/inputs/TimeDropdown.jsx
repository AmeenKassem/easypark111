import React, { useEffect, useLayoutEffect, useRef, useState } from "react";

/**
 * Reusable time dropdown that:
 * - shows up to `maxVisible` options (scrollable)
 * - renders in a fixed-position popover (not clipped by modal overflow)
 * - flips up if there isn't enough space below
 *
 * NOTE: options are passed in already-filtered (your logic stays unchanged).
 */
export default function TimeDropdown({
                                         value,
                                         onChange,
                                         options,
                                         placeholder = "--:--",
                                         disabled = false,
                                         maxVisible = 5,
                                     }) {
    const btnRef = useRef(null);
    const popRef = useRef(null);

    const [open, setOpen] = useState(false);
    const [pos, setPos] = useState({ left: 0, top: 0, width: 0, direction: "down" });

    const OPTION_HEIGHT = 44; // px
    const GAP = 6; // px
    const desiredHeight = Math.min(options.length + 1, maxVisible + 1) * OPTION_HEIGHT; // + placeholder row

    const computePosition = () => {
        const el = btnRef.current;
        if (!el) return;

        const r = el.getBoundingClientRect();
        const spaceBelow = window.innerHeight - r.bottom;
        const spaceAbove = r.top;

        const shouldFlipUp = spaceBelow < desiredHeight && spaceAbove > spaceBelow;

        const direction = shouldFlipUp ? "up" : "down";
        const left = Math.max(8, Math.min(r.left, window.innerWidth - r.width - 8));
        const width = r.width;

        const top = shouldFlipUp
            ? Math.max(8, r.top - desiredHeight - GAP)
            : Math.min(window.innerHeight - desiredHeight - 8, r.bottom + GAP);

        setPos({ left, top, width, direction });
    };

    useLayoutEffect(() => {
        if (!open) return;
        computePosition();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [open, desiredHeight]);

    useEffect(() => {
        if (!open) return;

        const onResize = () => computePosition();
        const onScroll = () => computePosition();
        const onKey = (e) => {
            if (e.key === "Escape") setOpen(false);
        };

        const onDocMouseDown = (e) => {
            const btn = btnRef.current;
            const pop = popRef.current;
            if (!btn || !pop) return;
            if (btn.contains(e.target) || pop.contains(e.target)) return;
            setOpen(false);
        };

        window.addEventListener("resize", onResize);
        window.addEventListener("scroll", onScroll, true);
        document.addEventListener("keydown", onKey);
        document.addEventListener("mousedown", onDocMouseDown);

        return () => {
            window.removeEventListener("resize", onResize);
            window.removeEventListener("scroll", onScroll, true);
            document.removeEventListener("keydown", onKey);
            document.removeEventListener("mousedown", onDocMouseDown);
        };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [open]);

    const display = value || "";

    return (
        <>
            <button
                ref={btnRef}
                type="button"
                className={`ep-timefield ${disabled ? "is-disabled" : ""}`}
                onClick={() => {
                    if (disabled) return;
                    setOpen((v) => !v);
                }}
                aria-haspopup="listbox"
                aria-expanded={open}
                disabled={disabled}
            >
        <span className={`ep-timefield__text ${display ? "" : "is-placeholder"}`}>
          {display || placeholder}
        </span>
                <span className="ep-timefield__caret" aria-hidden="true">▾</span>
            </button>

            {open && (
                <div
                    ref={popRef}
                    className={`ep-timemenu ${pos.direction === "up" ? "is-up" : "is-down"}`}
                    style={{ left: pos.left, top: pos.top, width: pos.width, maxHeight: desiredHeight }}
                    role="listbox"
                >
                    <button
                        type="button"
                        className={`ep-timeoption ${!value ? "is-active" : ""}`}
                        onClick={() => {
                            onChange("");
                            setOpen(false);
                        }}
                    >
                        {placeholder}
                    </button>

                    {options.map((t) => (
                        <button
                            key={t}
                            type="button"
                            className={`ep-timeoption ${t === value ? "is-active" : ""}`}
                            onClick={() => {
                                onChange(t);
                                setOpen(false);
                            }}
                        >
                            {t}
                        </button>
                    ))}
                </div>
            )}
        </>
    );
}
