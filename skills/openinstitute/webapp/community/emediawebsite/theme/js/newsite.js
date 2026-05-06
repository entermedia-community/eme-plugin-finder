document.addEventListener("DOMContentLoaded", function () {
	const prefersReducedMotion = window.matchMedia(
		"(prefers-reduced-motion: reduce)",
	).matches;

	const chatMessages = document.getElementById("chatMessages");
	if (chatMessages) {
		const chatForm = document.getElementById("chatForm");
		const chatBox = document.getElementById("chatBox");
		const firstMessage = document.getElementById("firstMessage");
		const choiceButtons = firstMessage.querySelectorAll(".choice");

		const starterThreads = {
			Personal: [
				{ role: "user", text: "Create a Personal eMe" },
				{
					role: "assistant",
					text: "Great choice. We will prioritize private notes, media memories, and reflection prompts.",
				},
				{
					role: "user",
					text: "I want weekly summaries and better recall of my notes.",
				},
				{
					role: "assistant",
					text: "Perfect. I will set your first workflow as: capture, summarize weekly, and surface key moments every Monday.",
				},
			],
			Commercial: [
				{ role: "user", text: "Create a Commercial eMe" },
				{
					role: "assistant",
					text: "Great. We will begin with team knowledge, SOPs, and role-based access controls.",
				},
				{
					role: "user",
					text: "Start with onboarding docs and support transcripts.",
				},
				{
					role: "assistant",
					text: "Excellent. I will create a workspace for onboarding and customer support with searchable response templates.",
				},
			],
		};

		function appendMessage(role, text) {
			const bubble = document.createElement("article");
			bubble.className = `msg ${role}`;
			bubble.textContent = text;
			chatMessages.appendChild(bubble);
			chatMessages.scrollTop = chatMessages.scrollHeight;
		}

		function playThread(mode) {
			const selected = starterThreads[mode] || [];
			let delay = 120;

			selected.forEach((item) => {
				delay += item.role === "assistant" ? 520 : 260;
				setTimeout(() => appendMessage(item.role, item.text), delay);
			});
		}

		choiceButtons.forEach((button) => {
			button.addEventListener("click", () => {
				if (firstMessage.dataset.locked === "true") {
					return;
				}

				const mode = button.dataset.mode;
				firstMessage.dataset.locked = "true";

				choiceButtons.forEach((choice) => {
					choice.classList.toggle("active", choice === button);
					choice.disabled = true;
				});

				playThread(mode);
				chatBox.focus();
			});
		});

		chatForm.addEventListener("submit", (event) => {
			event.preventDefault();
			const text = chatBox.value.trim();

			if (!text) {
				return;
			}

			appendMessage("user", text);
			chatBox.value = "";

			setTimeout(() => {
				appendMessage(
					"assistant",
					"Received. I can turn that into onboarding steps, priorities, and your first AI memory routines.",
				);
			}, 360);
		});
	}

	if (!prefersReducedMotion && window.gsap && window.ScrollTrigger) {
		gsap.registerPlugin(ScrollTrigger);

		gsap.set(".reveal", {
			y: 36,
			opacity: 0,
		});

		// gsap.to(".site-header", {
		// 	y: 16,
		// 	opacity: 1,
		// 	duration: 0.8,
		// 	ease: "power2.out",
		// 	scrollTrigger: {
		// 		trigger: ".site-header",
		// 		start: "top top",
		// 		scrub: true,
		// 	},
		// });

		// gsap.to(".site-header", {
		// 	y: 0,
		// 	opacity: 0.95,
		// 	duration: 0.8,
		// 	ease: "power2.out",
		// 	scrollTrigger: {
		// 		trigger: ".site-header",
		// 		start: "top top",
		// 		end: "+=200",
		// 		scrub: true,
		// 	},
		// });

		const heroTimeline = gsap.timeline({ defaults: { ease: "power3.out" } });

		heroTimeline
			.from(".brand, .site-nav a, .site-header .button", {
				y: -18,
				opacity: 0,
				duration: 0.6,
				stagger: 0.08,
			})
			.to(
				".hero .reveal",
				{
					y: 0,
					opacity: 1,
					duration: 0.9,
					stagger: 0.12,
				},
				"-=0.25",
			)
			.from(
				".hero-card--primary",
				{
					scale: 0.9,
					rotation: -5,
					opacity: 0,
					duration: 1,
				},
				"-=0.8",
			)
			.from(
				".hero-card--secondary, .hero-badge",
				{
					y: 40,
					opacity: 0,
					duration: 0.9,
					stagger: 0.1,
				},
				"-=0.65",
			);

		gsap.utils.toArray(".section").forEach((section) => {
			const reveals = section.querySelectorAll(".reveal:not(.engine-card)");

			if (reveals.length && !section.classList.contains("hero")) {
				gsap.to(reveals, {
					y: 0,
					opacity: 1,
					duration: 0.9,
					stagger: 0.08,
					ease: "power2.out",
					scrollTrigger: {
						trigger: section,
						start: "top 75%",
					},
				});
			}
		});

		gsap.utils.toArray(".parallax").forEach((element) => {
			const speed = Number(element.dataset.speed || 0.18);

			gsap.fromTo(
				element,
				{ yPercent: -speed * 100 },
				{
					yPercent: speed * 100,
					ease: "none",
					scrollTrigger: {
						trigger: element,
						start: "top bottom",
						end: "bottom top",
						scrub: true,
					},
				},
			);
		});

		const enginesCards = gsap.utils.toArray(".engine-card");

		gsap.fromTo(
			enginesCards,
			{
				y: 60,
				opacity: 0,
				rotateX: -10,
			},
			{
				y: 0,
				opacity: 1,
				rotateX: 0,
				duration: 0.9,
				stagger: 0.12,
				ease: "power2.out",
				scrollTrigger: {
					trigger: "#engines",
					start: "top 65%",
				},
			},
		);

		gsap.fromTo(
			".ownership-panel",
			{ y: 24 },
			{
				y: 0,
				ease: "none",
				scrollTrigger: {
					trigger: "#ownership",
					start: "top bottom",
					end: "bottom top",
					scrub: true,
				},
			},
		);

		var newHeight = window.innerWidth * (1080 / 1920);
		var newWidth = window.innerWidth;
		if (newHeight < window.innerHeight) {
			newHeight = window.innerHeight;
			newWidth = window.innerHeight * (1920 / 1080);
		}
		$(".hero-bg").css({ opacity: 0.05 });
		gsap.set("#bg1, #bg2", {
			width: newWidth,
			height: newHeight,
		});

		var bgTl = gsap.timeline({ repeat: -1 });
		bgTl.fromTo(
			"#bg1",
			{
				y: -newHeight,
			},
			{
				duration: 30,
				y: 0,
				ease: "none",
			},
		);
		bgTl.fromTo(
			"#bg2",
			{
				y: 0,
			},
			{
				duration: 30,
				y: newHeight,
				ease: "none",
			},
			"<",
		);
	}
});
