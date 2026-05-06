document.addEventListener("DOMContentLoaded", function () {
	gsap.config({
		nullTargetWarn: false,
	});
	gsap.registerPlugin(MotionPathPlugin);
	var imageCoords = {
		0: [-655, -345],
		1: [725, -55],
		2: [770, -310],
		3: [-380, 40],
		4: [-780, -70],
		5: [-470, -170],
		6: [-840, -230],
		7: [300, 80],
		8: [220, 420],
		9: [510, 20],
		10: [-190, -250],
		18: [-530, 310],
		12: [700, 310],
		13: [540, -250],
		14: [-300, 220],
		15: [430, 210],
		16: [370, -270],
		17: [210, -290],
		11: [-850, 400],
		19: [-600, -40],
		20: [-240, 380],
		21: [-145, 260],
		22: [220, 280],
		23: [410, 360],
		24: [80, 430],
		25: [-120, -430],
		26: [460, -430],
		27: [-280, -350],
		28: [-410, -330],
	};

	var initTL = gsap.timeline({ delay: 1 });
	initTL.fromTo(
		"#emlogo",
		{ y: -1100 },
		{
			y: -350,
			scale: 0.8,
			transformOrigin: "center",
			duration: 1,
			ease: "bounce.out",
		},
	);
	// initTL.fromTo(
	// 	"#caption",
	// 	{ y: 200, opacity: 0 },
	// 	{ y: 50, opacity: 1, duration: 1 },
	// 	"<"
	// );
	// initTL.to("#caption", { y: 100, duration: 1, ease: "expo.in" }, "+=1");
	initTL.to(
		"#emlogo",
		{
			y: -200,
			scale: 1,
			transformOrigin: "center",
			duration: 1,
			ease: "expo.in",
		},
		"<",
	);

	// gsap.to(".anBG", { opacity: 0, delay: 3 });
	// gsap.to(".anBG", { opacity: 0.05, delay: 18 });

	initTL.set("#images", { opacity: 1 });
	initTL.set("#folders", { opacity: 1 });

	// initTL.to("#intro_1", { opacity: 0, duration: 0.5 });
	// initTL.to("#intro_rect", { opacity: 0, duration: 0.5 }, "<");

	// initTL.to("#intro_2", { opacity: 1, duration: 0.5 }, "+=2.2");
	// initTL.to("#intro_rect", { opacity: 1, duration: 0.5 }, "<");

	// initTL.to("#intro_2", { opacity: 0, duration: 0.5 }, "+=4");
	// initTL.to("#intro_rect", { opacity: 0, duration: 0.5 }, "<");

	// initTL.to("#intro_3", { opacity: 1, duration: 0.5 }, "+=1");
	// initTL.to("#intro_rect", { opacity: 1, duration: 0.5 }, "<");

	// initTL.to("#intro_3", { opacity: 0, duration: 0.5 }, "+=3");
	// initTL.to("#intro_rect", { opacity: 0, duration: 0.5 }, "<");

	// initTL.to("#intro_4", { opacity: 1, duration: 0.5 }, "+=1");
	// initTL.to("#intro_rect", { opacity: 1, duration: 0.5 }, "<");

	// initTL.to("#intro_4", { opacity: 0, duration: 0.5 }, "+=2");
	// initTL.to("#intro_rect", { opacity: 0, duration: 0.5 }, "<");

	var images = document.querySelectorAll("#images image");

	gsap.set("#images image", { scale: 0 });
	var imgTl = gsap.timeline({
		ease: "none",
		delay: 4,
	});
	images.forEach((image, idx) => {
		var coords = imageCoords[idx];
		imgTl.to(
			image,
			{
				duration: 1,
				x: coords[0],
				y: coords[1],
				scale: 1,
				ease: "back.out(1.2)",
				onComplete: function () {
					image.classList.add("glow");
				},
			},
			"-=0.85",
		);
	});
	images.forEach((image, i) => {
		var coords = imageCoords[i];
		imgTl.to(
			image,
			{
				duration: 5,
				motionPath: {
					path: [
						{
							x: coords[0] + 16,
							y: coords[1],
						},
						{
							x: coords[0] + 16,
							y: coords[1] + 16,
						},
						{
							x: coords[0],
							y: coords[1] + 16,
						},
						{
							x: coords[0],
							y: coords[1],
						},
					],
				},
				repeat: -1,
				yoyo: true,
				ease: "none",
			},
			"-=4.75",
		);
	});

	var tl = gsap.timeline({
		ease: "none",
		delay: 6.5,
		onComplete: function () {
			images.forEach((img) => {
				img.classList.remove("glow");
			});
		},
		onReverseComplete: function () {
			var endTL = gsap.timeline();
			endTL.to("#folders", {
				opacity: 0,
				duration: 0,
			});
			endTL.to(
				"#emlogo",
				{
					// y: -500,
					scale: 2.5,
					transformOrigin: "center",
					duration: 1,
					ease: "expo.in",
				},
				"<",
			);
			// endTL.to("#introHero", { y: -50, opacity: 1, duration: 1 }, "+=0.5");
			// endTL.to(
			// 	"#introHero text",
			// 	{ opacity: 1, stagger: 0.1, duration: 0.25 },
			// 	"<"
			// );
		},
	});
	var folders = [
		"#anEvents",
		"#anLocations",
		"#anPeople",
		"#anProducts",
		"#anProjects",
	];
	for (let i = 0; i < folders.length; i++) {
		tl.to(
			folders[i],
			{
				duration: 2,
				motionPath: {
					path: "#f" + i,
					align: "#f" + i,
					autoRotate: false,
					alignOrigin: [0.5, 0.5],
				},
				ease: "none",
			},
			"<",
		);
	}

	var collapseCoords = {
		".people_img": {
			x: 290,
			y: 290,
			target: "#anPeople",
			delay: 6,
		},
		".location_img": {
			x: 360,
			y: -145,
			target: "#anLocations",
			delay: 0,
		},
		".event_img": {
			x: 0,
			y: -375,
			target: "#anEvents",
			delay: 0,
		},
		".project_img": {
			x: -372,
			y: -70,
			target: "#anProjects",
			delay: 4,
		},
		".product_img": {
			x: -160,
			y: 380,
			target: "#anProducts",
			delay: 0,
		},
	};

	Object.keys(collapseCoords).forEach((key, idx) => {
		var cc = collapseCoords[key];
		var images = document.querySelectorAll(key);

		var imTL = gsap.timeline({
			delay: idx === 0 ? 9.5 : 14,
			onStart: function () {
				images.forEach((img) => {
					img.classList.add("glow");
				});
			},
			onComplete: function () {
				if (key === ".product_img") tl.reverse();
			},
		});

		images.forEach((img) => {
			imTL.to(
				img,
				{
					duration: idx === 0 ? 1 : 1.5,
					x: cc.x,
					y: cc.y,
					scale: 0.2,
					transformOrigin: "center",
					onComplete: function () {
						img.remove();
					},
				},
				"-=0.75",
			);
			if (idx === 0) {
				imTL.fromTo(
					cc.target,
					{ scale: 1.1 },
					{ scale: 1, duration: 0.5, ease: "elastic.out(1, 0.3)" },
					"-=0.2",
				);
			}
		});

		// 	gsap.to("#bg1", { duration: 3 });
		// 	gsap.to("#bg2", { duration: 3 });
		// 	var bgTl = gsap.timeline({ repeat: -1 });
		// 	bgTl.fromTo(
		// 		"#bg1",
		// 		{
		// 			yPercent: -100,
		// 		},
		// 		{
		// 			duration: 30,
		// 			yPercent: 0,
		// 			ease: "none",
		// 		}
		// 	);
		// 	bgTl.fromTo(
		// 		"#bg2",
		// 		{
		// 			yPercent: 0,
		// 		},
		// 		{
		// 			duration: 30,
		// 			yPercent: 100,
		// 			ease: "none",
		// 		},
		// 		"<"
		// 	);
	});
	// $("#createOwn").click(function () {
	// 	window.location.href = "/builder/";
	// });
	// $("#freeLibs").click(function () {
	// 	window.location.href = "/libraries/";
	// });
});
