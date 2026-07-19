/**
 * Short project overview for the About section.
 */
export default function AboutPanel() {
  return (
    <div className="rgf-about">
      <h2>About Kinetile 🌱⚡</h2>
      <p className="rgf-about-lead">
        Cities are already full of motion. Kinetile asks a hopeful question: can some of that everyday movement be turned into
        something useful? ✨
      </p>

      <div className="rgf-about-body">
        <section className="rgf-about-section" aria-labelledby="about-what">
          <h3 id="about-what">What this project explores 🧭</h3>
          <p>
            Kinetile estimates the micro-energy potential of piezoelectric surfaces in busy urban environments of footsteps, bike
            and scooters across sidewalks, stations, campuses, promenades, and crossings. The goal is to see
            whether that motion could help power small local devices: sensors, LED markers, smart signage, environmental monitors,
            and other low-power infrastructure.
          </p>
          <p>
            In other words, could a crowded sidewalk help:<br />
            🚦 light a crossing signal<br />
            🌍 support an air-quality sensor<br />
            🪧 power a street sign<br />
            or at the very least earn the pavement some bragging rights? 😄
          </p>
        </section>

        <section className="rgf-about-section" aria-labelledby="about-scope">
          <h3 id="about-scope">Realistic scope 🎯</h3>
          <p>
            Kinetile is not about powering an entire city, it is about a more realistic, inspiring possibility: <br />
            Using energy that is already produced in daily life, without asking anyone to change their routine. <br />
            People are already moving through the city so the question is whether that movement can quietly contribute something good in return. <br />
          </p>
        </section>

        <section className="rgf-about-section" aria-labelledby="about-behavior">
          <h3 id="about-behavior">No bonus-level stomping required 🎮👣</h3>
          <p>
            No one needs to walk faster, stomp harder, or dramatically leap onto a glowing tile like they are unlocking a bonus
            level in a video game.<br />
            The movement is already there, Kinetile simply explores whether technology can make a little bit of
            good come out of it.
          </p>
        </section>

        <section className="rgf-about-section" aria-labelledby="about-honesty">
          <h3 id="about-honesty">💚 Honest framing 💚</h3>
          <p>
            Kinetile is a <strong>simulation and feasibility model</strong>, not a live power plant or certified
            field measurement.
            <br />The Numbers on the dashboard estimate how much energy piezo tiles <em>could</em> harvest
            from footfall and light mobility, then compare those totals against the daily load of small edge devices.
          </p>
          <p>
            The technology is still developing, and this is not a perfect solution (yet).<br />
            But new green ideas have to start somewhere! 😎 <br />
            Even small contributions deserve attention, especially when they happen with no extra effort from the
            people making them possible 💚
          </p>
        </section>

        <p className="rgf-about-outro">
          Kinetile is a celebration of that idea: a small, practical step toward cleaner, smarter, and more creative urban
          infrastructure 🌆⚡
        </p>
      </div>
    </div>
  )
}
