<a name="readme-top"></a>
# RMI Implementation

<!-- TABLE OF CONTENTS -->
<details>
  <summary>Table of Contents</summary>
  <ol>
    <li><a href="#about-the-project">About The Project</a></li>
    <li>
      <a href="#getting-started">Getting Started</a>
      <ul>
        <li><a href="#prerequisites">Prerequisites</a></li>
        <li><a href="#running-the-program">Running the Program</a></li>
      </ul>
    </li>
    <li><a href="#sample-output">Sample Output</a></li>
    <li><a href="#contact">Contact</a></li>
    <li><a href="#acknowledgments">Acknowledgments</a></li>
  </ol>
</details>


<!-- ABOUT THE PROJECT -->
## About The Project

This program implements Java RMI to send messages between two objects, between the Field Unit and the Central Server.

![problemdesc-screenshot]

*Background: This program simulates Sensors sending (random) measurements to a Field Unit, who will use Java RMI to send the moving average readings to Central Server

<p align="right">(<a href="#readme-top">back to top</a>)</p>


<!-- GETTING STARTED -->
## Getting Started

### Prerequisites

Use intellij ideally. :)

### Running the program

1. Clone the repo
   ```sh
   git clone https://github.com/liangsiwei1994/RMIImplementation.git
   ```
2. Open the terminal. Go to the folder/directory path and compile the program using the following command
   ```js
   make
   ```
3. Run the Central Server first using:
   ```js
   ./centralServer.sh
   ```
4. Then on a separate terminal, run the field unit using
   ```js
   ./fieldunit.sh <port # to listen on for incoming sensor message> <RMI address of central server>
   ```
6. Lastly on a separate terminal, run the sensor using
   ```js
   ./sensor.sh <IP address of field unit> <port number field unit is listening on> <number of measures to send>
   ```

<p align="right">(<a href="#readme-top">back to top</a>)</p>



<!-- SAMPLE OUTPUT -->
## Sample Output

The following is a sample output displayed on each of the terminals (for sensor, field unit and central server respectively, when the sensor is simulated to send 50 messages

![sampleoutput-screenshot][sampleoutput-screenshot]

<p align="right">(<a href="#readme-top">back to top</a>)</p>


<!-- CONTACT -->
## Contact

SiWei - liang_siwei@hotmail.com

<p align="right">(<a href="#readme-top">back to top</a>)</p>



<!-- ACKNOWLEDGMENTS -->
## Acknowledgments

Credits to setter of the project and the readme template designer vi-dev0.

* [Professor Emil Lupu](https://www.imperial.ac.uk/people/e.c.lupu)
* [Luca Castiglione](https://www.linkedin.com/in/ecleipteon/)
* [vi-dev0](https://github.com/othneildrew/Best-README-Template.git)

<p align="right">(<a href="#readme-top">back to top</a>)</p>



<!-- MARKDOWN LINKS & IMAGES -->
[problemdesc-screenshot]: images/problemDescription.png
[sampleoutput-screenshot]: images/sampleOutput.png
