var splashcontainer = document.createElement('div');
splashcontainer.id = "splashscreen";

var splashContent = document.createElement('div');

var image = document.createElement("img");
image.src = "launcher-icons/iot-core.svg";

var title = document.createElement("span");
title.className = "title";
title.innerHTML = "IoT vending machine";

var description = document.createElement("span");
description.className = "description";
description.innerHTML = "IoT vending machine";

var progress = document.createElement("div");
progress.className = "progress";

var progressFill = document.createElement("div");
progressFill.className = "indeterminate";
progress.append(progressFill);

splashContent.append(image);
splashContent.append(title);
splashContent.append(description);
splashContent.append(progress);

splashcontainer.append(splashContent);
document.body.appendChild(splashcontainer);

document.write('<link rel="stylesheet" type="text/css" href="splash/splash.css">');
